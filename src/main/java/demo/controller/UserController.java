package demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import demo.common.BizException;
import demo.common.Result;
import demo.model.User;
import demo.service.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "用户管理", description = "用户的增删改查操作")
public class UserController {

    private final UserServiceImpl userService;

    public UserController(UserServiceImpl userService) {
        this.userService = userService;
    }

    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询用户", description = "支持按姓名、年龄、手机号、时间范围筛选")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功，返回分页数据"),
            @ApiResponse(responseCode = "400", description = "参数错误（页码或页大小不合法）")
    })
    public Result<IPage<User>> pageQuery(
            @Parameter(description = "页码，从1开始", example = "1")
            @Min(value = 1, message = "页码不能小于1") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "每页大小，范围1-100", example = "10")
            @Min(value = 1, message = "页大小不能小于1") @Max(value = 100, message = "页大小不能大于100") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "姓名（模糊查询）", required = false)
            @RequestParam(required = false) String name,
            @Parameter(description = "年龄", required = false)
            @RequestParam(required = false) Integer age,
            @Parameter(description = "手机号", required = false)
            @RequestParam(required = false) String phone,
            @Parameter(description = "开始时间", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) LocalDateTime start,
            @Parameter(description = "结束时间", example = "2024-12-31T23:59:59")
            @RequestParam(required = false) LocalDateTime end) {
        return Result.success(userService.pageQuery(pageNo, pageSize, name, age, phone, start, end));
    }

    /**
     * 条件搜索用户
     */
    @GetMapping("/search")
    @Operation(summary = "条件搜索用户", description = "不分页，返回所有符合条件的用户列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "搜索成功，返回用户列表")
    })
    public Result<List<User>> search(
            @Parameter(description = "姓名（模糊查询）")
            @RequestParam(required = false) String name,
            @Parameter(description = "年龄")
            @RequestParam(required = false) Integer age,
            @Parameter(description = "手机号")
            @RequestParam(required = false) String phone,
            @Parameter(description = "开始时间")
            @RequestParam(required = false) LocalDateTime start,
            @Parameter(description = "结束时间")
            @RequestParam(required = false) LocalDateTime end) {
        return Result.success(userService.pageQuery(1, Integer.MAX_VALUE, name, age, phone, start, end).getRecords());
    }

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户，需提供姓名、年龄、手机号")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "422", description = "参数校验失败（姓名、年龄、手机号格式不正确）"),
            @ApiResponse(responseCode = "500", description = "创建失败")
    })
    public Result<Boolean> create(@Validated(User.CreateGroup.class) @RequestBody User user) {
        boolean saved = userService.save(user);
        if (!saved) {
            throw new BizException("用户创建失败");
        }
        return Result.success(true);
    }

    /**
     * 更新用户
     */
    @PutMapping
    @Operation(summary = "更新用户", description = "更新用户信息，需提供ID和版本号（乐观锁）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "422", description = "参数校验失败或版本冲突"),
            @ApiResponse(responseCode = "500", description = "更新失败")
    })
    public Result<Boolean> update(@Validated(User.UpdateGroup.class) @RequestBody User user) {
        boolean updated = userService.updateById(user);
        if (!updated) {
            throw new BizException("用户更新失败，可能版本号过期或记录不存在");
        }
        return Result.success(true);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "逻辑删除用户，不会真正从数据库删除")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public Result<Boolean> delete(
            @Parameter(description = "用户ID", example = "1")
            @PathVariable Long id) {
        boolean removed = userService.removeById(id);
        if (!removed) {
            throw new BizException("用户不存在");
        }
        return Result.success(true);
    }
}
