package org.lambda.framework.common.support;

import org.lambda.framework.common.exception.EventException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_027;

public interface PrincipalStash {
    public Mono<SecurityStash> setSecurityStash();

    @Component
    @ConditionalOnMissingBean(PrincipalStash.class)
    public static class Stash implements PrincipalStash{

        @Override
        public Mono<SecurityStash> setSecurityStash() {
            throw new EventException(ES_COMMON_027);
        }
    }

}
