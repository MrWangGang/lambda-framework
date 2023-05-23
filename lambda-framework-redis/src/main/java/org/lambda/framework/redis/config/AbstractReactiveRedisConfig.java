package org.lambda.framework.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.util.Strings;
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
public abstract class AbstractReactiveRedisConfig {

        //##Redis服务器地址
        protected abstract String host();
        //## Redis服务器连接端口
        protected abstract Integer port();

        //连接池密码
        protected abstract String password();

        //# 连接池最大连接数
        protected abstract Integer maxActive();

        //# 连接池最大阻塞等待时间（使用负值表示没有限制）
        protected abstract Integer maxWaitSeconds();

        //# 连接池中的最大空闲连接
        protected abstract Integer maxIdle();

        //# 连接池中的最小空闲连接
        protected abstract Integer minIdle();
        //##数据库序号
        protected abstract Integer database();

        public ReactiveRedisTemplate redisTemplate() {
            RedisSerializationContext.SerializationPair<String> stringSerializationPair = RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer.UTF_8);
            Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(new ObjectMapper(),Object.class);

            RedisSerializationContext.RedisSerializationContextBuilder builder =
                    RedisSerializationContext.newSerializationContext();
            builder.key(stringSerializationPair);
            builder.value(valueSerializer);
            builder.hashKey(stringSerializationPair);
            builder.hashValue(valueSerializer);
            builder.string(stringSerializationPair);
            RedisSerializationContext build = builder.build();
            ReactiveRedisTemplate reactiveRedisTemplate = new ReactiveRedisTemplate(redisConnectionFactory(), build);
            return reactiveRedisTemplate;
        }

        private ReactiveRedisConnectionFactory redisConnectionFactory(){
            RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
            redisStandaloneConfiguration.setDatabase(database());
            redisStandaloneConfiguration.setHostName(host());
            redisStandaloneConfiguration.setPort(port());
            if (Strings.isNotBlank(password())) {
                redisStandaloneConfiguration.setPassword(RedisPassword.of(password()));
            }
            // 连接池配置
            GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
            genericObjectPoolConfig.setMaxTotal(maxActive());
            genericObjectPoolConfig.setMaxIdle(maxIdle());
            genericObjectPoolConfig.setMinIdle(minIdle());
            genericObjectPoolConfig.setMaxWait(Duration.ofSeconds(maxWaitSeconds()));
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

