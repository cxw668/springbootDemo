package demo;

import lombok.Data;

@Data
public class ValidationErrorDetail {
    private String field;
    private String message;
    private Object rejectedValue;

    public ValidationErrorDetail(String field, String message, Object rejectedValue) {
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
    }
}
