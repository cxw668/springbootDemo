package demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import demo.common.BizCode;
import demo.common.BizException;
import demo.model.User;
import demo.mapper.UserMapper;
import demo.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author cxw_p
 * @since 2026-04-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    private final FileUploadServiceImpl fileUploadServiceImpl;
    private final PasswordEncoder passwordEncoder;

    public IPage<User> pageQuery(int pageNo, int pageSize, String name, Integer age, String phone, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(name), User::getName, name)
                .eq(age != null, User::getAge, age)
                .like(StringUtils.isNotBlank(phone), User::getPhone, phone)
                .between(start != null && end != null, User::getCreateTime, start, end)
                .orderByDesc(User::getUpdateTime);

        return this.page(new Page<>(pageNo, pageSize), wrapper);
    }
    @Override
    public void updateAvatar(Long userId, String avatarPath) {
        User user = getById(userId);
        if(user == null) throw new BizException(BizCode.NOT_FOUND_USER);
        // 删除旧头像
        if (StringUtils.isNotBlank(user.getAvatar())) {
            fileUploadServiceImpl.deleteFile(user.getAvatar());
        }
        // 更新新头像
        user.setAvatar(avatarPath);
        updateById(user);
    }

    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return getOne(wrapper);
    }

    @Override
    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return count(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(User user) {
        // 1. 检查用户名是否已存在
        if (existsByUsername(user.getUsername())) {
            throw new BizException(BizCode.USER_ALREADY_EXISTS);
        }

        // 2. 加密密码
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 3. 设置默认角色
        if (StringUtils.isBlank(user.getRole())) {
            user.setRole("USER");
        }

        // 4. 保存用户
        save(user);
        log.info("用户注册成功: {}", user.getUsername());

        // 5. 返回用户信息（不包含密码）
        User result = new User();
        result.setId(user.getId());
        result.setUsername(user.getUsername());
        result.setName(user.getName());
        result.setEmail(user.getEmail());
        result.setRole(user.getRole());
        result.setCreateTime(user.getCreateTime());
        
        return result;
    }
}
