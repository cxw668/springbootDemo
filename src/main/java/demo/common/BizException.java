package demo.common;

import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = BizCode.INTERNAL_ERROR.getCode();
    }

    public BizException(BizCode bizCode, String message) {
        super(message);
        this.code = bizCode.getCode();
    }

    public BizException(BizCode bizCode) {
        super(bizCode.getMessage());
        this.code = bizCode.getCode();
    }
}
