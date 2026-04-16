package demo.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 功能：
 * 1. 注册请求日志拦截器和鉴权拦截器
 * 2. 配置全局 CORS 跨域策略
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor requestLoggingInterceptor;
    private final SimpleAuthInterceptor simpleAuthInterceptor;

    public WebMvcConfig(RequestLoggingInterceptor requestLoggingInterceptor,
                        SimpleAuthInterceptor simpleAuthInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
        this.simpleAuthInterceptor = simpleAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 请求日志拦截器（所有请求）
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/**");

        // 鉴权拦截器（排除白名单路径）
        registry.addInterceptor(simpleAuthInterceptor)
                .addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
