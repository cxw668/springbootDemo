package demo.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 鉴权相关配置属性
 */
@Component
@ConfigurationProperties(prefix = "security.auth")
public class AuthProperties {

    /** 有效 token（演示用，后续替换为 JWT） */
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
