package org.lambda.framework.common.support;

import reactor.core.publisher.Mono;

public interface PrincipalStash {
    public Mono<SecurityStash> setSecurityStash();
}
