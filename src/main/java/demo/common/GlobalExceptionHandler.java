package demo.common;

import demo.ValidationErrorDetail;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBizException(BizException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验失败 (@Valid on @RequestBody)
     * 返回结构化错误详情
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<List<ValidationErrorDetail>> handleValidation(MethodArgumentNotValidException e) {
        List<ValidationErrorDetail> errors = e.getBindingResult().getFieldErrors().stream()
                .map(err -> new ValidationErrorDetail(err.getField(), err.getDefaultMessage(), err.getRejectedValue()))
                .collect(Collectors.toList());
        String message = errors.stream()
                .map(err -> err.getField() + ": " + err.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);
        Result<List<ValidationErrorDetail>> result = Result.fail(BizCode.VALIDATION_ERROR.getCode(), message);
        result.setData(errors);
        return result;
    }

    /**
     * 路径变量/请求参数校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", message);
        return Result.fail(BizCode.VALIDATION_ERROR.getCode(), message);
    }

    /**
     * 缺少必需请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getParameterName());
        return Result.fail(BizCode.BAD_REQUEST.getCode(), "缺少参数: " + e.getParameterName());
    }

    /**
     * 参数错误（如文件格式不匹配）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.fail(BizCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 404 资源不存在
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        return Result.fail(BizCode.NOT_FOUND.getCode(), e.getMessage());
    }

    /**
     * 兜底：未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        return Result.fail(BizCode.INTERNAL_ERROR.getCode(), "服务器内部错误");
    }
}
