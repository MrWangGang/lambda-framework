package org.lambda.framework.rsocket.support;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.SecurityStash;
import org.lambda.framework.compliance.security.PrincipalFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.lambda.framework.common.enums.ConmonContract.AUTHTOKEN_STASH_NAMING;
import static org.lambda.framework.common.enums.ConmonContract.PRINCIPAL_STASH_NAMING;
import static org.lambda.framework.rsocket.enums.RsocketExceptionEnum.*;

@Component
public class RsocketPrincipalFactory extends PrincipalFactory {

    @Override
    public Mono<String> getAuthToken(){
        return getRequest(AUTHTOKEN_STASH_NAMING);
    }

    @Override
    protected Mono<String> fetchSubject() {
        return getRequest(PRINCIPAL_STASH_NAMING);
    }

    private Mono<String> getRequest(String key) {
        return Mono.deferContextual(Mono::just)
                .map(contextView -> {
                    Object securityStash = contextView.get(SecurityStash.class);
                    Assert.verify(securityStash,ES_RSOCKET_003);
                    switch (key){
                        case AUTHTOKEN_STASH_NAMING:
                            String authToken = ((SecurityStash)securityStash).getAuthToken();
                            Assert.verify(authToken,ES_RSOCKET_001);
                            return authToken;
                        case PRINCIPAL_STASH_NAMING:
                            String principal = ((SecurityStash)securityStash).getPrincipal();
                            Assert.verify(principal,ES_RSOCKET_002);
                            return principal;
                        default:throw new EventException(ES_RSOCKET_003);
                    }
                });  // 使用 RSocketRequester 获取元数据
    }
}
