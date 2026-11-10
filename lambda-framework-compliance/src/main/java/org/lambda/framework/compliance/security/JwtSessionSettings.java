package org.lambda.framework.compliance.security;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

@ConfigurationProperties("lambda.security.jwt")
record JwtSessionSettings(
        String issuer,
        String appAudience,
        String adminAudience,
        @DefaultValue("lambda:auth:session") String redisPrefix,
        @DefaultValue("15m") Duration lifetime,
        Map<String, Resource> publicKeys,
        String signingKeyId,
        Resource privateKey) {}
