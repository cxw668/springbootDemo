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
}
