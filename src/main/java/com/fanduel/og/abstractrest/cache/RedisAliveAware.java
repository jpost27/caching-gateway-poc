package com.fanduel.og.abstractrest.cache;

import org.springframework.data.redis.connection.RedisConnectionFactory;

public interface RedisAliveAware {

    void setConnectionFactory(RedisConnectionFactory connectionFactory);

    boolean isAlive();
}
