/*
package org.lamb.framework.redis;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.util.Strings;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.common.util.sample.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.lamb.framework.common.enums.LambExceptionEnum.ES00000080;

@Component
@ConfigurationProperties(prefix = "org.lamb.redis")
@Getter
@Setter
public class LambRedisDataSourceFactory implements EnvironmentAware,BeanDefinitionRegistryPostProcessor{
    Logger logger = LoggerFactory.getLogger(LambRedisDataSourceFactory.class);
    private List<LambRedisDataBase> datasources;

    private static Environment environment;

    // 绑定配置信息
    @Override
    public void setEnvironment(Environment environment) {
        BindResult<List<LambRedisDataBase>> bindResult = Binder.get(environment).bind("org.lamb.redis.datasources", Bindable.listOf(LambRedisDataBase.class));
        if(bindResult!=null && bindResult.get()!=null&&bindResult.get().size()>0){
            datasources = bindResult.get();
        }
    }
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        if(datasources!=null && datasources.size()>0) {
            logger.info("lamb redis datasources bean 动态注入 by LambRedisDataSourceFactory.postProcessBeanDefinitionRegistry");
            datasources.stream().forEach(e->{
                if(StringUtil.isBlank(e.getName()))throw new LambEventException(ES00000080);
                //单机版配置
                RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
                redisStandaloneConfiguration.setDatabase(e.getNum());
                redisStandaloneConfiguration.setHostName(e.getHost());
                redisStandaloneConfiguration.setPort(e.getPort());
                redisStandaloneConfiguration.setPassword(RedisPassword.of(e.getPassword()));
                if (Strings.isNotBlank(e.getPassword())) {
                    redisStandaloneConfiguration.setPassword(RedisPassword.of(e.getPassword()));
                }
                // 连接池配置
                GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
                genericObjectPoolConfig.setMaxTotal(e.getPool().getMaxActive());
                genericObjectPoolConfig.setMaxIdle(e.getPool().getMaxIdle());
                genericObjectPoolConfig.setMinIdle(e.getPool().getMinIdle());
                genericObjectPoolConfig.setMaxWaitMillis(e.getPool().getMaxWait());

                // lettuce pool
                LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                        .poolConfig(genericObjectPoolConfig)
                        .build();
                LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
                lettuceConnectionFactory.setShareNativeConnection(false);
                ReactiveRedisConnectionFactory redisConnectionFactory =  lettuceConnectionFactory;

                //配置 ReactiveRedisTemplate 序列化
                RedisSerializationContext.SerializationPair<String> stringSerializationPair = RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer.UTF_8);
                RedisSerializationContext.SerializationPair<Object> objectSerializationPair = RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json());
                RedisSerializationContext.RedisSerializationContextBuilder builder = RedisSerializationContext.newSerializationContext();
                builder.key(stringSerializationPair);
                builder.value(objectSerializationPair);
                builder.hashKey(stringSerializationPair);
                builder.hashValue(objectSerializationPair);
                builder.string(objectSerializationPair);
                RedisSerializationContext build = builder.build();

                //注册bean
                GenericBeanDefinition bean =new GenericBeanDefinition();
                bean.setBeanClass(ReactiveRedisTemplate.class);
                bean.getPropertyValues().add("connectionFactory",redisConnectionFactory);
                bean.getPropertyValues().add("serializationContext",build);
                beanDefinitionRegistry.registerBeanDefinition(e.getName(),bean);
            });
        }
    }




    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        logger.info("redis datasource bean 动态注入 by LambRedisDataSourceFactory.postProcessBeanFactory");
    }

}
*/
