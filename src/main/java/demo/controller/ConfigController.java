package demo.controller;

import demo.common.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "配置接口", description = "提供应用配置信息")
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {
    private final AppProperties appProperties;

    @Operation(summary = "获取应用配置", description = "返回当前激活的应用配置信息")
    @GetMapping("/app")
    public Map<String, Object> getAppConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("name", appProperties.getName());
        config.put("version", appProperties.getVersion());
        config.put("description", appProperties.getDescription());
        config.put("defaultPageSize", appProperties.getDefaultPageSize());
        config.put("maxPageSize", appProperties.getMaxPageSize());
        config.put("cors", appProperties.getCors());
        return config;
    }

    @Operation(summary = "获取安全配置", description = "返回鉴权相关配置（脱敏）")
    @GetMapping("/security")
    public Map<String, Object> getSecurityConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("authType", "JWT");
        config.put("ipWhitelistSize", appProperties.getCors().getAllowedOrigins().length());
        config.put("corsEnabled", true);
        return config;
    }

    @Operation(summary = "获取所有配置摘要", description = "返回配置概览")
    @GetMapping("/summary")
    public Map<String, Object> getConfigSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("app", getAppConfig());
        summary.put("security", getSecurityConfig());
        summary.put("activeProfile", System.getProperty("spring.profiles.active", "default"));
        return summary;
    }
}
