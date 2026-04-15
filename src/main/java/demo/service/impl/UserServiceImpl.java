package demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import demo.common.Phone;
import demo.model.User;
import demo.mapper.UserMapper;
import demo.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author cxw_p
 * @since 2026-04-14
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    public IPage<User> pageQuery(int pageNo, int pageSize, String name, Integer age, String phone, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(name), User::getName, name)
                .eq(age != null, User::getAge, age)
                .like(StringUtils.isNotBlank(phone), User::getPhone, phone)
                .between(start != null && end != null, User::getCreateTime, start, end)
                .orderByDesc(User::getUpdateTime);

        return this.page(new Page<>(pageNo, pageSize), wrapper);
    }
}
