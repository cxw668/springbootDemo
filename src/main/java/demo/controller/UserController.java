package demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import demo.model.User;
import demo.service.IUserService;
import demo.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author cxw_p
 * @since 2026-04-14
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserServiceImpl userService;

    /**
     * 分页查询用户
     *
     * @param pageNo   页码，默认1
     * @param pageSize 每页大小，默认10
     * @param name     姓名（模糊查询）
     * @param age      年龄
     * @param start    开始时间
     * @param end      结束时间
     * @return 分页结果
     */
    @GetMapping("/page")
    public IPage<User> pageQuery(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {
        return userService.pageQuery(pageNo, pageSize, name, age, start, end);
    }

    /**
     * 条件搜索用户
     *
     * @param name  姓名（模糊查询）
     * @param age   年龄
     * @param start 开始时间
     * @param end   结束时间
     * @return 用户列表
     */
    @GetMapping("/search")
    public List<User> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {
        return userService.pageQuery(1, Integer.MAX_VALUE, name, age, start, end).getRecords();
    }

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 是否成功
     */
    @PostMapping
    public boolean create(@RequestBody User user) {
        return userService.save(user);
    }

    /**
     * 更新用户（包含乐观锁）
     *
     * @param user 用户信息（需包含id和version）
     * @return 是否成功
     */
    @PutMapping
    public boolean update(@RequestBody User user) {
        return userService.updateById(user);
    }

    /**
     * 删除用户（逻辑删除）
     *
     * @param id 用户ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return userService.removeById(id);
    }
}
