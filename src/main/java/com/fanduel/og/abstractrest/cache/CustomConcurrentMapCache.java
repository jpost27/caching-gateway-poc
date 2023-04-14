package com.fanduel.og.abstractrest.cache;

import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.serializer.support.SerializationDelegate;

import java.util.concurrent.ConcurrentMap;

public class CustomConcurrentMapCache extends ConcurrentMapCache {
    public CustomConcurrentMapCache(
            String name,
            ConcurrentMap<Object, Object> store,
            boolean allowNullValues,
            SerializationDelegate serialization) {
        super(name, store, allowNullValues, serialization);
    }
}
