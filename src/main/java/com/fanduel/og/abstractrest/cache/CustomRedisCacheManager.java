package com.fanduel.og.abstractrest.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;

@Slf4j
public class CustomRedisCacheManager implements CacheManager {

    protected RedisAliveAware connectionAliveAware;

    private CacheManager redisCacheManager;

    private CacheManager basicCacheManager;

    public CustomRedisCacheManager(
            RedisAliveAware connectionAliveAware,
            CacheManager redisCacheManager,
            CacheManager basicCacheManager) {
        this.connectionAliveAware = connectionAliveAware;
        this.redisCacheManager = redisCacheManager;
        this.basicCacheManager = basicCacheManager;
    }

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

    public void setConnectionAliveAware(RedisAliveAware connectionAliveAware) {
        this.connectionAliveAware = connectionAliveAware;
    }
}
