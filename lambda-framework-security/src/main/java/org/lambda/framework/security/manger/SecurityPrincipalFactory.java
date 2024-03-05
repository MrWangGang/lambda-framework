package org.lambda.framework.security.manger;

import org.lambda.framework.common.enums.SecurityContract;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.security.PrincipalFactory;
import org.lambda.framework.security.manger.support.SecurityAuthToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.ES_COMPLIANCE_021;

@Component
public class SecurityPrincipalFactory extends PrincipalFactory {
    @Override
    protected Mono<String> getAuthToken(){
        return getRequest(SecurityContract.AUTH_TOKEN_NAMING);
    }
    Mono<String> principal(){
        return super.getPrincipal();
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
                        return Mono.error(new EventException(ES_COMPLIANCE_021));
                    }
                    return Mono.just(request.getHeaders().get(key).get(0));
                }).switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_021)));
    }

    private  Mono<String> getRequest() {
        return Mono.deferContextual(Mono::just)
                .map(contextView -> contextView.get(SecurityAuthToken.class))
                .flatMap(securityAuthToken->{
                    return Mono.just(securityAuthToken.getPrincipal());
                }).switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_021)));
    }
}
