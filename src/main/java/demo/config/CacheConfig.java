package demo.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class CacheConfig {

    public static final String USER_BY_ID_CACHE = "userById";
    public static final String USER_BY_USERNAME_CACHE = "userByUsername";

    /**
     * 缓存配置
     * @return
     * 1. 禁用缓存空值
     * 2. 缓存键序列化器，默认键值为字符串，便于阅读
     * 3. 缓存值序列化器，默认值为对象，便于存储对象
     * 4. 设置默认过期时间为 15分钟
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(15));
    }

    /**
     * 缓存管理器配置
     * @return
     * 1. 用户信息详情缓存设置过期时间为30分钟，基本信息变动较少
     * 2. 用户信息查询缓存设置过期时间为10分钟，确保登录鉴权时能较快感知到角色或状态的变更。
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RedisCacheConfiguration redisCacheConfiguration) {
        return builder -> builder
                .withCacheConfiguration(
                        USER_BY_ID_CACHE,
                        redisCacheConfiguration.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(
                        USER_BY_USERNAME_CACHE,
                        redisCacheConfiguration.entryTtl(Duration.ofMinutes(10)));
    }
}
