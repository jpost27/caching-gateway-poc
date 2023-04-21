package com.fanduel.og.abstractrest.cache;

import com.fanduel.og.abstractrest.cache.CacheDetailsConfig;
import com.fanduel.og.abstractrest.cache.CacheKey;
import com.fanduel.og.abstractrest.cache.CustomJedisConnectionFactory;
import com.fanduel.og.abstractrest.cache.CustomMultiCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
@Slf4j
@RequiredArgsConstructor
public class CacheConfig extends CachingConfigurerSupport {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.connectionReadTimeout}")
    private long connectionReadTimeout;

    @Value("${spring.redis.connectionTimeout}")
    private long connectionTimeout;

    @Value("${cache.default.ttl}")
    private long defaultCacheTtl;

    private final CacheDetailsConfig cacheDetailsConfig;

    @Bean
    @Primary
    public CacheManager cacheManager(CustomMultiCacheManager cacheManager) {
        return cacheManager;
    }

    @Bean
    public JedisConnectionFactory redisConnectionFactory(
            JedisClientConfiguration jedisClientConfiguration) {
        RedisStandaloneConfiguration redisStandaloneConfiguration =
                new RedisStandaloneConfiguration(redisHost, redisPort);

        return new CustomJedisConnectionFactory(
                redisStandaloneConfiguration, jedisClientConfiguration);
    }

    @Bean
    public JedisClientConfiguration redisClientConfiguration() {
        return JedisClientConfiguration.builder()
                .readTimeout(Duration.ofSeconds(connectionReadTimeout))
                .connectTimeout(Duration.ofSeconds(connectionTimeout))
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(cf);
        return redisTemplate;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        Map<String, RedisCacheConfiguration> redisCacheConfigurations = new HashMap<>();

        for (CacheKey key : CacheKey.values()) {
            CacheDetailsConfig.CacheDetails cacheConfig = cacheDetailsConfig.get(key);
            if (cacheConfig == null) {
                log.warn("Cache config not found for {}. Using default values.", key);
                redisCacheConfigurations.put(
                        key.name(),
                        createCacheConfiguration(defaultCacheTtl));
            } else {
                redisCacheConfigurations.put(
                        key.name(),
                        createCacheConfiguration(cacheConfig.getTtl()));
            }
        }
        RedisCacheManager redisCacheManager =
                RedisCacheManager.builder(redisConnectionFactory)
                        .cacheDefaults(createCacheConfiguration(defaultCacheTtl))
                        .withInitialCacheConfigurations(redisCacheConfigurations)
                        .build();
        redisCacheManager.initializeCaches();
        return redisCacheManager;
    }

    @Bean
    public SimpleCacheManager simpleCacheManager(Ticker ticker) {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        List<CaffeineCache> mapCaches = Arrays
                .stream(CacheKey.values())
                .map(cacheKey -> buildCache(cacheKey, ticker))
                .collect(Collectors.toList());
        simpleCacheManager.setCaches(mapCaches);
        simpleCacheManager.initializeCaches();
        return simpleCacheManager;
    }

    private CaffeineCache buildCache(CacheKey name, Ticker ticker) {
        return new CaffeineCache(name.name(), Caffeine.newBuilder()
                .expireAfterWrite(
                        Optional.ofNullable(
                                Optional.ofNullable(cacheDetailsConfig.get(name))
                                        .orElse(new CacheDetailsConfig.CacheDetails())
                                        .getTtl()).orElse(defaultCacheTtl),
                        TimeUnit.SECONDS)
                .ticker(ticker)
                .build());
    }

    private RedisCacheConfiguration createCacheConfiguration(long timeoutInSeconds) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(timeoutInSeconds));
    }

    @Bean
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

    // This stops the cacheable annotation throwing an error when it can't deserialize data from the
    // cache (i.e. if the
    // object has changed with a release) and instead just goes direct to the source and then
    // reaches. This should
    // allow us to do releases without having issues with stale data in the cache from the previous
    // version.
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Error getting {} from cache {}.", key, cache.getName(), exception);
            }
        };
    }
}
