package org.lambda.framework.repository.config.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.util.Strings;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.repository.enums.RedisDeployModelEnum;
import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;


public abstract class AbstractReactiveRedisRepositoryConfig {

        //##Redis服务器地址 xxxx:xx
        protected abstract String host();
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

        protected abstract String deployModel();

        protected abstract String masterName();

        public ReactiveRedisOperation buildRedisOperation() {
            Assert.verify(this.deployModel(), ES_REPOSITORY_REDIS_037);
            RedisSerializationContext.SerializationPair<String> stringSerializationPair = RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer.UTF_8);
            Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(JsonUtil.getJsonFactory(),Object.class);
            RedisSerializationContext.RedisSerializationContextBuilder builder =
                    RedisSerializationContext.newSerializationContext();
            builder.key(stringSerializationPair);
            builder.value(valueSerializer);
            builder.hashKey(stringSerializationPair);
            builder.hashValue(valueSerializer);
            builder.string(stringSerializationPair);
            RedisSerializationContext build = builder.build();
            ReactiveRedisOperation reactiveRedisOperation = new ReactiveRedisOperation(redisConnectionFactory(this.deployModel()), build);
            return reactiveRedisOperation;
        }

        private ReactiveRedisConnectionFactory redisConnectionFactory(String deployModel){
            Assert.verify(deployModel, ES_REPOSITORY_REDIS_037);
            RedisDeployModelEnum.isValid(deployModel);
            LettuceConnectionFactory lettuceConnectionFactory = null;
            switch (RedisDeployModelEnum.valueOf(deployModel)) {
                case single:
                    lettuceConnectionFactory = new LettuceConnectionFactory(this.single(),lettuceClientConfiguration());
                    break;
                case master_slave:
                    lettuceConnectionFactory = new LettuceConnectionFactory(this.masterSlave(),lettuceClientConfiguration());
                    break;
                case cluster:
                    lettuceConnectionFactory = new LettuceConnectionFactory(this.cluster(),lettuceClientConfiguration());
                    break;
                case sentinel:
                    lettuceConnectionFactory = new LettuceConnectionFactory(this.sentinel(),lettuceClientConfiguration());
                    break;
                default:
                    throw new EventException(ES_REPOSITORY_REDIS_040);
            }
            lettuceConnectionFactory.setShareNativeConnection(true);
            lettuceConnectionFactory.setValidateConnection(false);
            lettuceConnectionFactory.afterPropertiesSet();
            return lettuceConnectionFactory;
        }

    private RedisStaticMasterReplicaConfiguration masterSlave() {
        Assert.verify(this.host(),ES_REPOSITORY_REDIS_030);
        Assert.verify(this.database(),ES_REPOSITORY_REDIS_036);
        String[] nodes = this.host().split(",");
        boolean firstNode = true;
        RedisStaticMasterReplicaConfiguration redisStaticMasterReplicaConfiguration = null;
        for (String node : nodes) {
            String[] parts = node.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            if (firstNode) {
                redisStaticMasterReplicaConfiguration = new RedisStaticMasterReplicaConfiguration(host,port);
                firstNode = false;
            } else {
                redisStaticMasterReplicaConfiguration.addNode(host, port);
            }
        }
        redisStaticMasterReplicaConfiguration.setDatabase(this.database());
        if (Strings.isNotBlank(this.password())) {
            redisStaticMasterReplicaConfiguration.setPassword(RedisPassword.of(this.password()));
        }
        return redisStaticMasterReplicaConfiguration;
    }



    private RedisSentinelConfiguration sentinel() {
        Assert.verify(this.host(),ES_REPOSITORY_REDIS_030);
        Assert.verify(this.database(),ES_REPOSITORY_REDIS_036);
        Assert.verify(this.masterName(),ES_REPOSITORY_REDIS_039);
        String[] nodes = this.host().split(",");
        RedisSentinelConfiguration sentinelConfiguration = new RedisSentinelConfiguration();
        sentinelConfiguration.setMaster(this.masterName());
        for (String node : nodes) {
            String[] parts = node.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            sentinelConfiguration.sentinel(host, port);
        }
        sentinelConfiguration.setDatabase(this.database());
        if (Strings.isNotBlank(this.password())) {
            sentinelConfiguration.setPassword(RedisPassword.of(this.password()));
        }
        return sentinelConfiguration;
    }

    private RedisClusterConfiguration cluster(){
        //密码不需要校验
        Assert.verify(this.host(),ES_REPOSITORY_REDIS_030);
        Assert.verify(this.database(),ES_REPOSITORY_REDIS_036);
        String[] nodes = this.host().split(",");

        RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration();
        for (String node : nodes) {
            String[] parts = node.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            RedisNode redisNode = new RedisNode(host, port);
            redisClusterConfiguration.addClusterNode(redisNode);
        }
        if (Strings.isNotBlank(password())) {
            redisClusterConfiguration.setPassword(RedisPassword.of(password()));
        }
        return redisClusterConfiguration;
    }

        private RedisStandaloneConfiguration single(){
            //密码不需要校验
            Assert.verify(this.host(),ES_REPOSITORY_REDIS_030);
            Assert.verify(this.database(),ES_REPOSITORY_REDIS_036);
            String[] nodes = this.host().split(":");
            RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
            redisStandaloneConfiguration.setDatabase(database());
            redisStandaloneConfiguration.setHostName(nodes[0]);
            redisStandaloneConfiguration.setPort(Integer.valueOf(nodes[1]));
            if (Strings.isNotBlank(password())) {
                redisStandaloneConfiguration.setPassword(RedisPassword.of(password()));
            }
            return redisStandaloneConfiguration;
        }

        private LettuceClientConfiguration lettuceClientConfiguration() {
            Assert.verify(this.maxActive(),ES_REPOSITORY_REDIS_032);
            Assert.verify(this.maxWaitSeconds(),ES_REPOSITORY_REDIS_033);
            Assert.verify(this.maxIdle(),ES_REPOSITORY_REDIS_034);
            Assert.verify(this.minIdle(),ES_REPOSITORY_REDIS_035);
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
            return clientConfig;
        }
}

