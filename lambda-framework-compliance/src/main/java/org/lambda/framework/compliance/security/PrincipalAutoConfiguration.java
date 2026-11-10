package org.lambda.framework.compliance.security;

import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(JwtSessionSettings.class)
public class PrincipalAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(PrincipalFactory.class)
    PrincipalFactory principalFactory(ObjectProvider<ReactiveRedisOperation> redis,
            ObjectProvider<JwtSessionTokens> jwt) {
        return new PrincipalFactory(redis, jwt);
    }

    @Bean
    @ConditionalOnProperty(prefix = "lambda.security.jwt", name = "enabled", havingValue = "true")
    JwtSessionTokens jwtSessionTokens(JwtSessionSettings settings) {
        return new JwtSessionTokens(settings);
    }
}
