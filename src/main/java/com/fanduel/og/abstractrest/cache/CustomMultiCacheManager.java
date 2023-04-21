package com.fanduel.og.abstractrest.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
@Component
public class CustomMultiCacheManager implements CacheManager {

    private final RedisHealthChecker connectionAliveAware;
    private final RedisCacheManager redisCacheManager;
    private final SimpleCacheManager basicCacheManager;

    @Override
    public Cache getCache(String name) {
        if (this.connectionAliveAware != null && !this.connectionAliveAware.isAlive()) {
            log.warn(
                    "Using in-memory cache for {}",
                    name); // Or put this in RedisConnectionChecker::pingRedis()
            return basicCacheManager.getCache(name);
        }
        log.debug("Reading from redis cache for {}", name);
        return redisCacheManager.getCache(name);
    }

    @Override
    public Collection<String> getCacheNames() {
        if (this.connectionAliveAware != null && !this.connectionAliveAware.isAlive()) {
            return basicCacheManager.getCacheNames();
        }
        return redisCacheManager.getCacheNames();
    }
}
