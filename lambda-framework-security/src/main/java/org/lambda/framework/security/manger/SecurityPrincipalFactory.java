package org.lambda.framework.security.manger;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.security.PrincipalFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.lambda.framework.common.enums.ConmonContract.*;
import static org.lambda.framework.security.enums.SecurityExceptionEnum.*;

@Component
public class SecurityPrincipalFactory extends PrincipalFactory {
    @Override
    public Mono<String> getAuthToken(){
        return getRequest(AUTHTOKEN_STASH_NAMING);
    }
    Mono<String> principal(){
        return super.getPrincipal();
    }
    @Override
    protected Mono<String> fetchSubject() {
        return getRequest(PRINCIPAL_STASH_NAMING);
    }

    private  Mono<String> getRequest(String key) {
        return Mono.deferContextual(Mono::just)
                .map(contextView ->{
                    ServerWebExchange serverWebExchange = contextView.get(ServerWebExchange.class);
                    Assert.verify(serverWebExchange,ES_SECURITY_008);
                    Object token = serverWebExchange.getAttributes().get(key);
                    Assert.verify(token,ES_SECURITY_0014);
                    String value = token.toString();
                   return value;
                })
                .switchIfEmpty(Mono.error(new EventException(ES_SECURITY_0014)));
    }
}
