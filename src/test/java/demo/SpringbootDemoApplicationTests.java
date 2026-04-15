package demo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import demo.model.User;
import demo.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SpringbootDemoApplicationTests {

    @Autowired
    private UserServiceImpl userService;

    @Nested
    @DisplayName("Auto-fill behavior tests")
    class AutoFillTests {

        @Test
        @DisplayName("insertFill - createTime and updateTime are auto-filled on insert")
        void testAutoFillOnInsert() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            User user = new User();
            user.setName("AutoFillTest" + before);
            user.setAge(25);
            userService.save(user);

            assertNotNull(user.getCreateTime(), "createTime should be auto-filled");
            assertNotNull(user.getUpdateTime(), "updateTime should be auto-filled");
            assertTrue(user.getCreateTime().isAfter(before), "createTime should be recent");
            assertTrue(user.getUpdateTime().isAfter(before), "updateTime should be recent");
        }

        @Test
        @DisplayName("updateFill - updateTime is auto-filled on update")
        void testAutoFillOnUpdate() {
            User user = new User();
            user.setName("UpdateFillTest" + LocalDateTime.now());
            user.setAge(30);
            userService.save(user);

            LocalDateTime createTime = user.getCreateTime();
            LocalDateTime firstUpdateTime = user.getUpdateTime();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            user.setName("UpdatedName" + createTime);
            userService.updateById(user);

            assertNotNull(user.getUpdateTime(), "updateTime should be refreshed on update");
            assertTrue(user.getUpdateTime().isAfter(firstUpdateTime),
                    "updateTime should be later than previous updateTime");
        }
    }

    @Nested
    @DisplayName("Logical delete behavior tests")
    class LogicalDeleteTests {

        @Test
        @DisplayName("removeById - sets deleted flag to 1 instead of hard delete")
        void testLogicalDeleteSetsFlag() {
            User user = new User();
            user.setName("DeleteTest");
            user.setAge(20);
            userService.save(user);

            Long id = user.getId();
            assertEquals((byte) 0, user.getDeleted(), "Initial deleted flag should be 0");

            boolean removed = userService.removeById(id);
            assertTrue(removed, "removeById should return true");

            User deletedUser = userService.getById(id);
            assertNull(deletedUser, "getById should return null for logically deleted record");
        }

        @Test
        @DisplayName("query excludes logically deleted records")
        void testQueryExcludesDeletedRecords() {
            User user1 = new User();
            user1.setName("Visible1" + LocalDateTime.now());
            user1.setAge(22);
            userService.save(user1);

            User user2 = new User();
            user2.setName("Visible2" + LocalDateTime.now());
            user2.setAge(23);
            userService.save(user2);

            IPage<User> pageBefore = userService.pageQuery(1, 10, null, null, null, null);
            assertEquals(2, pageBefore.getTotal(), "Should have 2 records before delete");

            userService.removeById(user1.getId());

            IPage<User> pageAfter = userService.pageQuery(1, 10, null, null, null, null);
            assertEquals(1, pageAfter.getTotal(), "Should have 1 record after logical delete");
            assertEquals("Visible2", pageAfter.getRecords().get(0).getName());
        }
    }

    @Nested
    @DisplayName("Optimistic lock behavior tests")
    class OptimisticLockTests {

        @Test
        @DisplayName("updateById increments version on successful update")
        void testVersionIncrement() {
            User user = new User();
            LocalDateTime now = LocalDateTime.now();
            user.setName("VersionTest" + now);
            user.setAge(25);
            userService.save(user);

            Integer initialVersion = user.getVersion();
            assertNotNull(initialVersion, "Version should be set after insert");
            assertEquals(1, initialVersion, "Initial version should be 1");

            user.setName("UpdatedVersion" + now);
            userService.updateById(user);

            assertEquals(2, user.getVersion(), "Version should increment to 2 after update");
        }

        @Test
        @DisplayName("update with stale version fails")
        void testStaleVersionFails() {
            User user = new User();
            user.setName("ConcurrentTest");
            user.setAge(30);
            userService.save(user);

            Long id = user.getId();
            Integer originalVersion = user.getVersion();

            User staleCopy = userService.getById(id);
            staleCopy.setName("StaleUpdate");
            staleCopy.setVersion(originalVersion);

            boolean result = userService.updateById(staleCopy);
            assertFalse(result, "Update with stale version should fail");

            User freshUser = userService.getById(id);
            assertEquals("ConcurrentTest", freshUser.getName(),
                    "Name should not have changed after failed update");
        }
    }

    @Nested
    @DisplayName("Business logic tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("pageQuery - pagination returns correct page")
        void testPagination() {
            for (int i = 0; i < 10; i++) {
                User user = new User();
                user.setName("User" + i);
                user.setAge(20 + i);
                userService.save(user);
            }

            IPage<User> page = userService.pageQuery(1, 5, null, null, null, null);
            assertEquals(10, page.getTotal(), "Total should be 10");
            assertEquals(5, page.getSize(), "Page size should be 5");
            assertEquals(2, page.getPages(), "Should have 2 pages");
            assertEquals(5, page.getRecords().size(), "Should have 5 records on first page");
        }

        @Test
        @DisplayName("pageQuery - filters by name")
        void testFilterByName() {
            User user1 = new User();
            user1.setName("Alice");
            user1.setAge(25);
            userService.save(user1);

            User user2 = new User();
            user2.setName("Bob");
            user2.setAge(30);
            userService.save(user2);

            IPage<User> page = userService.pageQuery(1, 10, "Ali", null, null, null);
            assertEquals(1, page.getTotal(), "Should match only Alice");
            assertEquals("Alice", page.getRecords().get(0).getName());
        }

        @Test
        @DisplayName("pageQuery - filters by age")
        void testFilterByAge() {
            User user1 = new User();
            user1.setName("Young");
            user1.setAge(18);
            userService.save(user1);

            User user2 = new User();
            user2.setName("Old");
            user2.setAge(50);
            userService.save(user2);

            IPage<User> page = userService.pageQuery(1, 10, null, 18, null, null);
            assertEquals(1, page.getTotal(), "Should match only age 18");
        }

        @Test
        @DisplayName("batchInsert and deleteAll")
        void testBatchInsertAndDelete() {
            List<User> users = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                User user = new User();
                user.setName("BatchUser" + i);
                user.setAge(20 + i);
                users.add(user);
            }
            boolean saved = userService.saveBatch(users);
            assertTrue(saved, "Batch save should return true");

            IPage<User> page = userService.pageQuery(1, 10, null, null, null, null);
            assertEquals(5, page.getTotal(), "Should have 5 records");

            boolean deleted = userService.removeBatchByIds(
                    users.stream().map(User::getId).toList());
            assertTrue(deleted, "Batch delete should return true");

            IPage<User> afterDelete = userService.pageQuery(1, 10, null, null, null, null);
            assertEquals(0, afterDelete.getTotal(), "All records should be logically deleted");
        }
    }
}
