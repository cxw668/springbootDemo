package demo.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 */
@Getter
@AllArgsConstructor
public enum BizCode {

    OK(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    VALIDATION_ERROR(422, "参数校验失败"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;
}
