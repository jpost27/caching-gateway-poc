package com.fanduel.og.abstractrest;

import com.fanduel.og.abstractrest.cache.CacheDetailsConfig;
import com.fanduel.og.abstractrest.cache.CustomConcurrentMapCache;
import com.fanduel.og.abstractrest.cache.CustomJedisConnectionFactory;
import com.fanduel.og.abstractrest.cache.CustomRedisCacheManager;
import com.fanduel.og.abstractrest.cache.RedisConnectionChecker;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.serializer.support.SerializationDelegate;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Setup JedisConnectionFactory config bean for setting redis host + port RedisTemplate bean in
 * order to use RedisConnectionFactory JedisCacheConfiguration - so we can use spring
 * boot's @Cacheable etc on our service CacheManager - brings it all together to configure the cache
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig extends CachingConfigurerSupport {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.teamCacheTimeout}")
    private long teamCacheTimeout;

    @Value("${spring.redis.connectionReadTimeout}")
    private long connectionReadTimeout;

    @Value("${spring.redis.connectionTimeout}")
    private long connectionTimeout;

    @Value("${cache.default.ttl}")
    private long defaultCacheTtl;

    private final CacheDetailsConfig cacheDetailsConfig;

    public static final String SWAPI_CACHE_NAME = "swapi";

    public static final Map<String, Class<?>> cacheKeyList =
            Map.ofEntries(
                    Map.entry(SWAPI_CACHE_NAME, byte[].class));

    public CacheConfig(CacheDetailsConfig cacheDetailsConfig) {
        this.cacheDetailsConfig = cacheDetailsConfig;
    }

    private static RedisCacheConfiguration createCacheConfiguration(long timeoutInSeconds) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(timeoutInSeconds));
    }

    @Bean
    public JedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration =
                new RedisStandaloneConfiguration(redisHost, redisPort);

        return new CustomJedisConnectionFactory(
                redisStandaloneConfiguration, redisClientConfiguration());
    }

    private JedisClientConfiguration redisClientConfiguration() {
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
    public RedisCacheConfiguration cacheConfiguration() {
        return createCacheConfiguration(teamCacheTimeout);
    }

    private SimpleCacheManager createSimpleCacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();

        List<ConcurrentMapCache> mapCaches = new ArrayList<>();

        for (Map.Entry<String, Class<?>> entry : cacheKeyList.entrySet()) {
            Map<String, String> cacheConfig = cacheDetailsConfig.getCaches().get(entry.getKey());
            if (cacheConfig == null) {
//                throw new RuntimeException(
//                        "Error creating cache with name "
//                                + entry.getKey()
//                                + " - have you defined this cache in yaml?");
                mapCaches.add(
                        createMapCache(
                                entry.getKey(),
                                3600L,
                                100L,
                                entry.getValue()));
            } else {
                mapCaches.add(
                        createMapCache(
                                entry.getKey(),
                                Long.parseLong(cacheConfig.get("ttl")),
                                Long.parseLong(cacheConfig.get("maxValues")),
                                entry.getValue()));
            }
        }

        simpleCacheManager.setCaches(mapCaches);
        simpleCacheManager.initializeCaches();
        return simpleCacheManager;
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory redisConnectionFactory,
            RedisConnectionChecker connectionChecker) {
        Map<String, RedisCacheConfiguration> redisCacheConfigurations = new HashMap<>();

        for (Map.Entry<String, Class<?>> entry : cacheKeyList.entrySet()) {
            Map<String, String> cacheConfig = cacheDetailsConfig.getCaches().get(entry.getKey());

            if (cacheConfig == null) {
                throw new RuntimeException(
                        "Error creating cache with name "
                                + entry.getKey()
                                + " - have you defined this cache in yaml?");
            }
            redisCacheConfigurations.put(
                    entry.getKey(),
                    createCacheConfiguration(Long.parseLong(cacheConfig.get("ttl"))));
        }

        AbstractCacheManager redisCacheManager =
                RedisCacheManager.builder(redisConnectionFactory)
                        .cacheDefaults(createCacheConfiguration(defaultCacheTtl))
                        .withInitialCacheConfigurations(redisCacheConfigurations)
                        .build();
        redisCacheManager.initializeCaches();

        return new CustomRedisCacheManager(
                connectionChecker, redisCacheManager, createSimpleCacheManager());
    }

    private ConcurrentMapCache createMapCache(
            String cacheName, Long cacheTtl, Long maxValues, Class<?> cachedClass) {
        return new CustomConcurrentMapCache(
                cacheName,
                CacheBuilder.newBuilder()
                        .expireAfterWrite(cacheTtl, TimeUnit.SECONDS)
                        .maximumSize(maxValues)
                        .build()
                        .asMap(),
                true,
                new SerializationDelegate(
                        cachedClass
                                .getClassLoader())); // serialization delegate must be passed in to
        // ensure cached values are stored by value
        // instead of reference
    }

    // This stops the cacheable annotation throwing an error when it can't deserialize data from the
    // cache (i.e. if the
    // object has changed with a release) and instead just goes direct to the source and then
    // recaches. This should
    // allow us to do releases without having issues with stale data in the cache from the previous
    // version.
    private static class NonThrowingCacheErrorHandler extends SimpleCacheErrorHandler {
        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Error getting {} from cache {}.", key, cache.getName(), exception);
        }
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new NonThrowingCacheErrorHandler();
    }
}
