package demo.common;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * 鉴权相关配置属性
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "security.auth")
public class AuthProperties {

    /** 有效 token（演示用，后续替换为 JWT） */
    @NotBlank(message = "token 不能为空")
    private String token = "demo-token-12345";

    /** IP 白名单 */
    private List<String> ipWhitelist = new ArrayList<>();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getIpWhitelist() {
        return ipWhitelist;
    }

    public void setIpWhitelist(List<String> ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
    }
}
