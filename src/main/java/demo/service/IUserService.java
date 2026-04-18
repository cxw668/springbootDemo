package demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import demo.model.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author cxw_p
 * @since 2026-04-14
 */
public interface IUserService extends IService<User> {
    IPage<User> pageQuery(int pageNo, int pageSize, String name, Integer age, String phone, java.time.LocalDateTime start, java.time.LocalDateTime end);

    void updateAvatar(Long userId, String avatarPath);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User findByUsername(String username);

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return true-已存在，false-不存在
     */
    boolean existsByUsername(String username);

    /**
     * 注册用户
     *
     * @param user 用户信息
     * @return 注册后的用户
     */
    User register(User user);
}
