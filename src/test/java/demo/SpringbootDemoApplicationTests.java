package demo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import demo.model.User;
import demo.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SpringbootDemoApplicationTests {

    @Autowired
    private UserServiceImpl userService;
    @Test
    void pageQuery() {
        IPage<User> page = userService.pageQuery(1, 5, null, null, null, null);
        System.out.println("Query Result: " + page);
    }
}
