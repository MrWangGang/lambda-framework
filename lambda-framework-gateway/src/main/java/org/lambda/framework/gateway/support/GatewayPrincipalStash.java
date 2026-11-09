package org.lambda.framework.gateway.support;

import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.SecurityStash;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GatewayPrincipalStash implements PrincipalStash {

    @Override
    public Mono<SecurityStash> setSecurityStash() {
        return Mono.deferContextual(context -> Mono.justOrEmpty(context.<SecurityStash>getOrEmpty(SecurityStash.class)));
    }
}
