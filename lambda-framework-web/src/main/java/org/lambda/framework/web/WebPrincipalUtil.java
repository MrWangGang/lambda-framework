package org.lambda.framework.web;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.security.PrincipalFactory;
import org.lambda.framework.compliance.security.container.SecurityContract;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.ES_COMPLIANCE_021;
import static org.lambda.framework.compliance.security.container.SecurityContract.PRINCIPAL_STASH_NAMING;

@Component
public class WebPrincipalUtil extends PrincipalFactory {
    @Override
    protected Mono<String> getAuthToken(){
        return getTokenByRequest(SecurityContract.AUTH_TOKEN_NAMING);
    }

    @Override
    protected Mono<String> fetchPrincipal() {
        return getTokenByRequest(PRINCIPAL_STASH_NAMING);
    }
    private  Mono<String> getTokenByRequest(String key) {
        return Mono.deferContextual(Mono::just)
                .map(contextView -> contextView.get(ServerWebExchange.class).getRequest())
                .flatMap(request->{
                    List<String> headers = request.getHeaders().get(key);
                    if(headers == null || headers.isEmpty() || headers.get(0) == null){
                        return Mono.error(new EventException(ES_COMPLIANCE_021));
                    }
                    return Mono.just(request.getHeaders().get(key).get(0));
                }).switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_021)));
    }
}
