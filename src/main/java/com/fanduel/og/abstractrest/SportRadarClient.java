package com.fanduel.og.abstractrest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class SportRadarClient {

    private final RestTemplate webClient;

    @Cacheable(value = "swapi", sync = true)
    public byte[] fetchAndCache(String uri) {
        log.info("Fetching {}", uri);
        return webClient.getForObject(URI.create(uri), byte[].class);
    }
}
