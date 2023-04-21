package com.fanduel.og.abstractrest.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.Jedis;

@Slf4j
public class CustomJedisConnectionFactory extends JedisConnectionFactory {

    public CustomJedisConnectionFactory() {}

    public CustomJedisConnectionFactory(RedisStandaloneConfiguration redisStandaloneConfiguration) {
        super(redisStandaloneConfiguration);
    }

    public CustomJedisConnectionFactory(
            RedisStandaloneConfiguration redisStandaloneConfiguration,
            JedisClientConfiguration redisClientConfiguration) {
        super(redisStandaloneConfiguration, redisClientConfiguration);
    }

    // Note: can be used to help with retry logic
    @Override
    protected Jedis fetchJedisConnector() {
        return super.fetchJedisConnector();
    }
}
