package com.fanduel.og.abstractrest;

import com.fanduel.og.abstractrest.aspect.MonoCache;
import com.fanduel.og.abstractrest.mongo.MongoCacheWrapper;
import com.fanduel.og.abstractrest.mongo.MongoCacheWrapperRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.text.ParseException;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SportRadarClient {

    private final RestTemplate webClient;
    private final MongoCacheWrapperRepo mongoCacheWrapperRepo;
    private static final String KEY = "swapi";

//    @Cacheable(value = KEY, sync = true)
    @MonoCache
    public Mono<Object> fetchAndCache(String uri) {
//        if (Boolean.TRUE.equals(mongoCacheWrapperRepo.existsById(uri).block())) {
//            log.info("Retrieving request from L2 (Mongo) cache");
//            return mongoCacheWrapperRepo.findById(uri).map(MongoCacheWrapper::getData);
//        }
        log.info("Fetching {}", uri);
        String responseString = webClient.getForObject(URI.create(uri), String.class);
        return Mono.just(responseString);
    }
}
