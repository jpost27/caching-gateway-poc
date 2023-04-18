package com.fanduel.og.abstractrest;

import com.fanduel.og.abstractrest.mongo.MongoCacheWrapper;
import com.fanduel.og.abstractrest.mongo.MongoCacheWrapperRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    @Cacheable(value = KEY, sync = true)
    public Object fetchAndCache(String uri) {
        if (Boolean.TRUE.equals(mongoCacheWrapperRepo.existsById(uri).block())) {
            log.info("Retrieving request from L2 (Mongo) cache");
            return mongoCacheWrapperRepo.findById(uri).block().getData();
        }
        log.info("Fetching {}", uri);
        String responseString = webClient.getForObject(URI.create(uri), String.class);

        if (responseString != null) {
            CompletableFuture.runAsync(() -> {
                log.info("Saving request to L2 (Mongo) cache");
                Object res;
                try {
                    res = Document.parse(responseString);
                } catch (JsonParseException e) {
                    res = responseString;
                }
                mongoCacheWrapperRepo.save(new MongoCacheWrapper(
                        uri,
                        res
                )).block();
            });
        }
        return responseString;
    }
}
