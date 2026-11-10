package org.lambda.framework.compliance.security;

import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class PrincipalAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(PrincipalFactory.class)
    PrincipalFactory principalFactory(ObjectProvider<ReactiveRedisOperation> redis) {
        return new PrincipalFactory(redis);
    }
}
