package org.lamb.framework.web.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.util.Strings;
import org.lamb.framework.common.exception.LambEventException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

import static org.lamb.framework.common.enums.LambExceptionEnum.ES00000080;

public abstract class LambSecurityRedisConfig {
    //##Redis服务器地址
    protected String host;

    //## Redis服务器连接端口
    protected Integer port;
    //连接池密码
    protected String password;

    //# 连接池最大连接数
    protected Integer maxActive;
    //# 连接池最大阻塞等待时间（使用负值表示没有限制）
    protected Integer maxWaitSeconds;

    //# 连接池中的最大空闲连接
    protected Integer maxIdle;

    //# 连接池中的最小空闲连接
    protected Integer minIdle;

    @Value("${lamb.security.redis.host:0}")
    public void setHost(String host) {
        if(host.equals("0"))throw new LambEventException(ES00000080);
        this.host = host;
    }
    @Value("${lamb.security.redis.port:6379}")
    public void setPort(Integer port) {
        this.port = port;
    }
    @Value("${lamb.security.redis.password:}")
    public void setPassword(String password) {
        this.password = password;
    }
    @Value("${lamb.security.redis.lettuce.pool.max_active:8}")
    public void setMaxActive(Integer maxActive) {
        this.maxActive = maxActive;
    }

    @Value("${lamb.security.redis.lettuce.pool.max_wait_seconds:50}")
    public void setMaxWaitSeconds(Integer maxWaitSeconds) {
        this.maxWaitSeconds = maxWaitSeconds;
    }
    @Value("${lamb.security.redis.lettuce.pool.max_idle:8}")
    public void setMaxIdle(Integer maxIdle) {
        this.maxIdle = maxIdle;
    }
    @Value("${lamb.security.redis.lettuce.pool.min_idle:0}")
    public void setMinIdle(Integer minIdle) {
        this.minIdle = minIdle;
    }
    //##数据库序号
    protected Integer database;
    public abstract void setDatabase(Integer database);

    public ReactiveRedisTemplate lambSecurityRedisTemplate() {
        RedisSerializationContext.SerializationPair<String> stringSerializationPair = RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer.UTF_8);
        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        valueSerializer.setObjectMapper(new ObjectMapper());

        RedisSerializationContext.RedisSerializationContextBuilder builder =
                RedisSerializationContext.newSerializationContext();
        builder.key(stringSerializationPair);
        builder.value(valueSerializer);
        builder.hashKey(stringSerializationPair);
        builder.hashValue(valueSerializer);
        builder.string(stringSerializationPair);
        RedisSerializationContext build = builder.build();
        ReactiveRedisTemplate reactiveRedisTemplate = new ReactiveRedisTemplate(lambSecurityRedisConnectionFactory(), build);
        return reactiveRedisTemplate;
    }

    private ReactiveRedisConnectionFactory lambSecurityRedisConnectionFactory(){
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setDatabase(database);
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setPort(port);
        if (Strings.isNotBlank(password)) {
            redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
        }
        // 连接池配置
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxTotal(maxActive);
        genericObjectPoolConfig.setMaxIdle(maxIdle);
        genericObjectPoolConfig.setMinIdle(minIdle);
        genericObjectPoolConfig.setMaxWait(Duration.ofSeconds(maxWaitSeconds));
        // lettuce pool
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(genericObjectPoolConfig)
                .build();
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
        lettuceConnectionFactory.setShareNativeConnection(true);
        lettuceConnectionFactory.setValidateConnection(false);
        lettuceConnectionFactory.afterPropertiesSet();
        return lettuceConnectionFactory;
    }

}
