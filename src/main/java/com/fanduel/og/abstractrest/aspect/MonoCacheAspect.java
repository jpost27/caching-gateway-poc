package com.fanduel.og.abstractrest.aspect;

import com.fanduel.og.abstractrest.mongo.MongoCacheWrapper;
import com.fanduel.og.abstractrest.mongo.MongoCacheWrapperRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Aspect
@Slf4j
@RequiredArgsConstructor
public class MonoCacheAspect {

    private final MongoCacheWrapperRepo mongoCacheWrapperRepo;

    @Pointcut(value = "@annotation(MonoCache)")
    public void cacheMono(){}

//    @Before("cacheMono()")
//    public Mono<Object> beforeCacheMono(JoinPoint joinPoint){
//        System.out.println("In Aspect");
//        // TODO get variable better
//        String uri = (String) joinPoint.getArgs()[0];
//        if (Boolean.TRUE.equals(mongoCacheWrapperRepo.existsById(uri).block())) {
//            log.info("Retrieving request from L2 (Mongo) cache");
//            return mongoCacheWrapperRepo.findById(uri).map(MongoCacheWrapper::getData);
//        }
//        System.out.println("Leaving Aspect");
//        return null;
//    }

    @Around(value = "cacheMono()")
    public Mono<?> aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("In Around Aspect");
        Mono<?> returnMono = null;
        if ((returnMono = getFromL1Cache()) != null) {
            return returnMono;
        } else if ((returnMono = getFromL2Cache()) != null) {
            return returnMono;
        }
        try {
            returnMono = (Mono<?>) joinPoint.proceed();
        } catch (ClassCastException e) {
            log.error("{} should only be used with return type {}", MonoCache.class, Mono.class);
            throw e;
        }

        returnMono = returnMono.doOnSuccess(contents -> {
            populateL1Cache(contents);
            populateL2Cache("", contents);
        });

        return returnMono;
    }

    private <T> void populateL1Cache(T obj) {
        log.info("Populating L1 cache {}", obj);
    }

    private <T> T getFromL1Cache() {
        log.info("Retrieving from L1 cache");
        return null;
    }

    private <T> void populateL2Cache(String key, T obj) {
        log.info("Populating L2 cache {}", obj);
//        Object res;
//        try {
//            res = Document.parse(obj.toString());
//        } catch (JsonParseException e) {
//            res = obj.toString();
//        }
//        mongoCacheWrapperRepo.save(new MongoCacheWrapper(
//                key,
//                res
//        )).block();
    }

    private <T> T getFromL2Cache() {
        log.info("Retrieving from L2 cache");
        T res = null;
        if (res != null) {
            populateL1Cache(res);
        }
        return res;
    }
}