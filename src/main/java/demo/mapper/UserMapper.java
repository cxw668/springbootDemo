package demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import demo.model.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author cxw_p
 * @since 2026-04-14
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
