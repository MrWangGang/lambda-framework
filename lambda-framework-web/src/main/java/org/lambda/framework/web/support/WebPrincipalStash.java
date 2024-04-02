package org.lambda.framework.web.support;

import jakarta.annotation.Resource;
import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.SecurityStash;
import org.lambda.framework.compliance.security.SecurityPrincipalHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class WebPrincipalStash implements PrincipalStash {

    @Resource
    private SecurityPrincipalHolder securityPrincipalHolder;

    @Override
    public Mono<SecurityStash> setSecurityStash() {
        SecurityStash securityStash = SecurityStash.builder().build();
        return Mono.zip(securityPrincipalHolder.fetchPrincipal(), securityPrincipalHolder.getAuthToken())
                .flatMap(v -> {
                    securityStash.setPrincipal(v.getT1());
                    securityStash.setAuthToken(v.getT2());
                    return Mono.just(securityStash);
                });
    };
}
