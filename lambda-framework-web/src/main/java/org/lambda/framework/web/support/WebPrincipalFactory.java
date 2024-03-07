package org.lambda.framework.web.support;

import org.lambda.framework.common.enums.SecurityContract;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.security.PrincipalFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.lambda.framework.common.enums.SecurityContract.PRINCIPAL_STASH_NAMING;
import static org.lambda.framework.web.enums.WebExceptionEnum.ES_WEB_002;
import static org.lambda.framework.web.enums.WebExceptionEnum.ES_WEB_003;

@Component
public class WebPrincipalFactory extends PrincipalFactory {
    @Override
    protected Mono<String> getAuthToken(){
        return getRequest(SecurityContract.AUTH_TOKEN_NAMING);
    }

    @Override
    protected Mono<String> fetchSubject() {
        return getRequest();
    }
    private  Mono<String> getRequest(String key) {
        return Mono.deferContextual(Mono::just)
                .map(contextView -> contextView.get(ServerWebExchange.class).getRequest())
                .flatMap(request->{
                    List<String> headers = request.getHeaders().get(key);
                    if(headers == null || headers.isEmpty() || headers.get(0) == null){
                        return Mono.error(new EventException(ES_WEB_002));
                    }
                    return Mono.just(request.getHeaders().get(key).get(0));
                }).switchIfEmpty(Mono.error(new EventException(ES_WEB_002)));
    }

    private  Mono<String> getRequest() {
        return Mono.deferContextual(Mono::just)
                .map(contextView -> contextView.get(ServerWebExchange.class).getRequest())
                .flatMap(request->{
                    List<String> headers = request.getHeaders().get(PRINCIPAL_STASH_NAMING);
                    if(headers == null || headers.isEmpty() || headers.get(0) == null){
                        return Mono.error(new EventException(ES_WEB_003));
                    }
                    return Mono.just(request.getHeaders().get(PRINCIPAL_STASH_NAMING).get(0));
                }).switchIfEmpty(Mono.error(new EventException(ES_WEB_003)));
    }
}
