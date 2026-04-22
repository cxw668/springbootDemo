package demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private boolean enabled = true;

    private String provider = "SiliconFlow";

    private String defaultSystemPrompt = "你是一个有用的助手";
}
