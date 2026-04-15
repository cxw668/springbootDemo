package demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import demo.common.BizException;
import demo.common.Result;
import demo.model.User;
import demo.service.impl.UserServiceImpl;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
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
     * @param phone    手机号
     * @param start    开始时间
     * @param end      结束时间
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<IPage<User>> pageQuery(
            @Min(value = 1, message = "页码不能小于1") @RequestParam(defaultValue = "1") int pageNo,
            @Min(value = 1, message = "页大小不能小于1") @Max(value = 100, message = "页大小不能大于100") @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {
        return Result.success(userService.pageQuery(pageNo, pageSize, name, age, phone, start, end));
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
    public Result<List<User>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {
        return Result.success(userService.pageQuery(1, Integer.MAX_VALUE, name, age, phone, start, end).getRecords());
    }

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 是否成功
     */
    @PostMapping
    public Result<Boolean> create(@Validated(User.CreateGroup.class) @RequestBody User user) {
        boolean saved = userService.save(user);
        if (!saved) {
            throw new BizException("用户创建失败");
        }
        return Result.success(true);
    }

    /**
     * 更新用户（包含乐观锁）
     *
     * @param user 用户信息（需包含id和version）
     * @return 是否成功
     */
    @PutMapping
    public Result<Boolean> update(@Validated(User.UpdateGroup.class) @RequestBody User user) {
        boolean updated = userService.updateById(user);
        if (!updated) {
            throw new BizException("用户更新失败，可能版本号过期或记录不存在");
        }
        return Result.success(true);
    }

    /**
     * 删除用户（逻辑删除）
     *
     * @param id 用户ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean removed = userService.removeById(id);
        if (!removed) {
            throw new BizException("用户不存在");
        }
        return Result.success(true);
    }
}
