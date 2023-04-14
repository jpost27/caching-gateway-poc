package com.fanduel.og.abstractrest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("")
@Slf4j
@RequiredArgsConstructor
public class ExampleRest {

    private final SportRadarClient sportRadarClient;
    private final Map<String, String> apiKeys = Map.of(
            "nfl", "nujy5je6td2ucj2zjrpwwh98",
            "nba", "2hy55br4nryzd444yv7fj9kn",
            "mlb", "yfvwkdgaqu7xgdxtfjbrqtmk",
            "ncaamb", "cu69he2wrngtgjvdaam7nnyn",
            "ncaafb", "5av9mpnhbhrs6sqqbbpfmgvq",
            "ncaawb", "ybyrqvrb7earbth93thgkm6f",
            "nhl", "5hbgwfmkf3bffvn2kmvksraz",
            "wnba", "wfmcbu8th44xzun9ms56v43n"
            );

    @GetMapping(path = {
            "/{path1}",
            "/{path1}/{path2}",
            "/{path1}/{path2}/{path3}",
            "/{path1}/{path2}/{path3}/{path4}",
            "/{path1}/{path2}/{path3}/{path4}/{path5}",
            "/{path1}/{path2}/{path3}/{path4}/{path5}/{path6}",
            "/{path1}/{path2}/{path3}/{path4}/{path5}/{path6}/{path7}",
            "/{path1}/{path2}/{path3}/{path4}/{path5}/{path6}/{path7}/{path8}",
            "/{path1}/{path2}/{path3}/{path4}/{path5}/{path6}/{path7}/{path8}/{path9}",
            "/{path1}/{path2}/{path3}/{path4}/{path5}/{path6}/{path7}/{path8}/{path9}/{path10}",
            "/{path1}/{path2}/{path3}/{path4}/{path5}/{path6}/{path7}/{path8}/{path9}/{path10}/{path11}",
            "/{path1}/{path2}/{path3}/{path4}/{path5}/{path6}/{path7}/{path8}/{path9}/{path10}/{path11}/{path12}"},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<byte[]> get(
            @PathVariable("path1") String path1,
            @PathVariable("path2") Optional<String> path2,
            @PathVariable("path3") Optional<String> path3,
            @PathVariable("path4") Optional<String> path4,
            @PathVariable("path5") Optional<String> path5,
            @PathVariable("path6") Optional<String> path6,
            @PathVariable("path7") Optional<String> path7,
            @PathVariable("path8") Optional<String> path8,
            @PathVariable("path9") Optional<String> path9,
            @PathVariable("path10") Optional<String> path10,
            @PathVariable("path11") Optional<String> path11,
            @PathVariable("path12") Optional<String> path12,
            @RequestParam MultiValueMap<String, String> params
            ) {
        String league;
        if ((league = parseLeague(path1)) != null) {
            path1 = path1.toLowerCase();
        } else if (path2.isPresent() && (league = parseLeague(path2.get())) != null) {
            path2 = Optional.of(path2.get().toLowerCase());
        }
        if (league == null) {
            throw new IllegalArgumentException("league not supported");
        }
        List<String> paths = Stream.of(
                        Optional.of(path1),
                        path2,
                        path3,
                        path4,
                        path5,
                        path6,
                        path7,
                        path8,
                        path9,
                        path10,
                        path11,
                        path12
                ).flatMap(Optional::stream).collect(Collectors.toList());
        String path = Strings.join(paths, '/');
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        if (paths.get(paths.size() - 1).endsWith(".xml")) {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
        } else {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
        return new ResponseEntity<>(
                sportRadarClient.fetchAndCache(buildUri(path, params, league)),
                headers,
                HttpStatus.OK
        );
    }

    private String parseLeague(String path) {
        path = path.split("-")[0].toLowerCase();
        if (apiKeys.containsKey(path)) {
            return path;
        }
        return null;
    }

    private String buildUri(String path, MultiValueMap<String, String> params, String league) {
        return UriComponentsBuilder.fromUriString("http://api.sportradar.us")
                .path(path)
                .queryParams(params)
                .queryParam("api_key", apiKeys.get(league))
                .toUriString();
    }

}
