package demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置属性类
 * 
 * 使用方式：
 * 在 application.yml 中配置：
 * jwt:
 *   secret: your-secret-key-here-must-be-at-least-32-characters
 *   expiration: 7200000
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 密钥（至少 32 个字符）
     */
    private String secret = "mySecretKeyForSpringBootDemo2024VeryLong";

    /**
     * Token 过期时间（毫秒），默认 2 小时
     */
    private Long expiration = 7200000L;
}
