package com.fanduel.og.abstractrest.cache;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(value = "cache")
@Data
@NoArgsConstructor
public class CacheDetailsConfig {

    private Map<String, CacheDetails> map;

    public CacheDetails get(CacheKey key) {
        return map.get(key.name());
    }

    @Data
    public static class CacheDetails {
        private Long ttl;
        private Integer maxValues;
    }
}
