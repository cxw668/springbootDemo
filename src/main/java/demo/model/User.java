package demo.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import demo.common.Phone;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author cxw_p
 * @since 2026-04-14
 */
@Getter
@Setter
@ToString
@TableName("`user`")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（更新时必须提供）
     */
    @TableId("id")
    @NotNull(message = "ID不能为空", groups = UpdateGroup.class)
    private Long id;

    /**
     * 姓名
     */
    @TableField("name")
    @NotBlank(message = "姓名不能为空", groups = CreateGroup.class)
    @Size(min = 1, max = 50, message = "姓名长度必须在1-50之间", groups = CreateGroup.class)
    private String name;

    /**
     * 年龄
     */
    @TableField("age")
    @NotNull(message = "年龄不能为空", groups = CreateGroup.class)
    @Min(value = 0, message = "年龄不能小于0", groups = CreateGroup.class)
    @Max(value = 150, message = "年龄不能超过150", groups = CreateGroup.class)
    private Integer age;

    /**
     * 手机号
     */
    @TableField("phone")
    @Phone(message = "手机号格式不正确", groups = CreateGroup.class)
    private String phone;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识：0-未删除，1-已删除
     */
    @TableLogic
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Byte deleted;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;

    /**
     * 校验分组：创建
     */
    public interface CreateGroup {}

    /**
     * 校验分组：更新
     */
    public interface UpdateGroup {}
}
