package demo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class AiConfigurationTests {

    @Test
    @DisplayName("AI base URL defaults do not append /v1 in shared config")
    void sharedConfigShouldUseRootOpenAiCompatibleBaseUrl() {
        Properties properties = loadYaml("application.yaml");

        assertThat(properties.getProperty("spring.ai.openai.base-url"))
                .doesNotContain("/v1")
                .contains("https://api.siliconflow.cn");
    }

    @Test
    @DisplayName("AI base URL defaults do not append /v1 in dev config")
    void devConfigShouldUseRootOpenAiCompatibleBaseUrl() {
        Properties properties = loadYaml("application-dev.yml");

        assertThat(properties.getProperty("spring.ai.openai.base-url"))
                .doesNotContain("/v1")
                .contains("https://api.siliconflow.cn");
    }

    private Properties loadYaml(String classpathResource) {
        YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
        factoryBean.setResources(new ClassPathResource(classpathResource));
        Properties properties = factoryBean.getObject();
        assertThat(properties).isNotNull();
        return properties;
    }
}
