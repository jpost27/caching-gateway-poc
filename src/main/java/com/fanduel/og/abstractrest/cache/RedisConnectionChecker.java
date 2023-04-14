package com.fanduel.og.abstractrest.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@EnableScheduling
public class RedisConnectionChecker implements RedisAliveAware {

    private boolean isAlive = false;

    private RedisConnectionFactory connectionFactory;

    public RedisConnectionChecker(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    protected void init() {
        pingRedis();
    }

    @Scheduled(fixedRate = 5000)
    public void pingRedis() {
        try {
            RedisConnection redisConnection = connectionFactory.getConnection();
            isAlive = "PONG".equals(redisConnection.ping());
        } catch (RedisConnectionFailureException e) {
            isAlive = false;
        }
    }

    public boolean isAlive() {
        return isAlive;
    }

    @Override
    public void setConnectionFactory(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
}
