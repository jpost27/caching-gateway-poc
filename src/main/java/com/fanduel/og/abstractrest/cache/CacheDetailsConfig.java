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

    private Map<String, Map<String, String>> map;

    public Map<String, Map<String, String>> getCaches() {
        return this.map;
    }
}
