package demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import demo.mapper.UserMapper;
import demo.model.User;
import demo.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试
 * 使用 Mockito 模拟 Mapper 层，独立测试 Service 逻辑
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setName("张三");
        testUser1.setAge(25);
        testUser1.setPhone("13800138001");
        testUser1.setCreateTime(LocalDateTime.now().minusDays(1));
        testUser1.setUpdateTime(LocalDateTime.now().minusDays(1));

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setName("李四");
        testUser2.setAge(30);
        testUser2.setPhone("13800138002");
        testUser2.setCreateTime(LocalDateTime.now());
        testUser2.setUpdateTime(LocalDateTime.now());
    }

    @Nested
    @DisplayName("分页查询测试")
    class PageQueryTests {

        @Testr
        @DisplayName("无条件分页查询 - 返回所有数据")
        void testPageQueryNoConditions() {
            // Arrange
            Page<User> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Arrays.asList(testUser1, testUser2));
            mockPage.setTotal(2);

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            // Act
            IPage<User> result = userService.pageQuery(1, 10, null, null, null, null, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(2);
            assertThat(result.getTotal()).isEqualTo(2);
            verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("按名称模糊查询")
        void testPageQueryByName() {
            // Arrange
            Page<User> mockPage = new Page<>(1, 10);
            mockPage.setRecords(List.of(testUser1));
            mockPage.setTotal(1);

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            // Act
            IPage<User> result = userService.pageQuery(1, 10, "张", null, null, null, null);

            // Assert
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("按年龄精确查询")
        void testPageQueryByAge() {
            // Arrange
            Page<User> mockPage = new Page<>(1, 10);
            mockPage.setRecords(List.of(testUser2));
            mockPage.setTotal(1);

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            // Act
            IPage<User> result = userService.pageQuery(1, 10, null, 30, null, null, null);

            // Assert
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getAge()).isEqualTo(30);
        }

        @Test
        @DisplayName("按手机号模糊查询")
        void testPageQueryByPhone() {
            // Arrange
            Page<User> mockPage = new Page<>(1, 10);
            mockPage.setRecords(List.of(testUser1));
            mockPage.setTotal(1);

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            // Act
            IPage<User> result = userService.pageQuery(1, 10, null, null, "13800", null, null);

            // Assert
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getPhone()).startsWith("13800");
        }

        @Test
        @DisplayName("多条件组合查询")
        void testPageQueryMultipleConditions() {
            // Arrange
            Page<User> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0);

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            // Act
            IPage<User> result = userService.pageQuery(1, 10, "王", 20, null, null, null);

            // Assert
            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("空结果分页")
        void testPageQueryEmptyResult() {
            // Arrange
            Page<User> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0);

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            // Act
            IPage<User> result = userService.pageQuery(1, 10, "不存在", null, null, null, null);

            // Assert
            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Nested
    @DisplayName("MyBatis-Plus IService 继承方法测试")
    class InheritedServiceMethodTests {

        @Test
        @DisplayName("根据ID查询用户")
        void testGetById() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser1);

            // Act
            User result = userService.getById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("张三");
            verify(userMapper).selectById(1L);
        }

        @Test
        @DisplayName("查询不存在的用户返回null")
        void testGetByIdNotFound() {
            // Arrange
            when(userMapper.selectById(999L)).thenReturn(null);

            // Act
            User result = userService.getById(999L);

            // Assert
            assertThat(result).isNull();
            verify(userMapper).selectById(999L);
        }

        @Test
        @DisplayName("保存用户")
        void testSave() {
            // Arrange
            when(userMapper.insert(any(User.class))).thenReturn(1);

            User newUser = new User();
            newUser.setName("王五");
            newUser.setAge(28);
            newUser.setPhone("13900139000");

            // Act
            boolean result = userService.save(newUser);

            // Assert
            assertThat(result).isTrue();
            verify(userMapper).insert(any(User.class));
        }

        @Test
        @DisplayName("根据ID删除用户")
        void testRemoveById() {
            // Arrange
            when(userMapper.deleteById(1L)).thenReturn(1);

            // Act
            boolean result = userService.removeById(1L);

            // Assert
            assertThat(result).isTrue();
            verify(userMapper).deleteById(1L);
        }

        @Test
        @DisplayName("更新用户信息")
        void testUpdateById() {
            // Arrange
            testUser1.setName("张三Updated");
            testUser1.setAge(26);
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            // Act
            boolean result = userService.updateById(testUser1);

            // Assert
            assertThat(result).isTrue();
            verify(userMapper).updateById(testUser1);
        }
    }
}
