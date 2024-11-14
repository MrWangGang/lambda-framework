package org.lambda.framework.compliance.security;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.SecurityStash;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_027;
import static org.lambda.framework.common.enums.ConmonContract.AUTHTOKEN_STASH_NAMING;
import static org.lambda.framework.common.enums.ConmonContract.PRINCIPAL_STASH_NAMING;
import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.*;

@Component
public class SecurityPrincipalHolder extends PrincipalFactory {

    @Component
    @ConditionalOnMissingBean(PrincipalStash.class)
    public static class Stash implements PrincipalStash{

        @Override
        public Mono<SecurityStash> setSecurityStash() {
            throw new EventException(ES_COMMON_027);
        }
    }

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
                    SecurityStash securityStash = contextView.get(SecurityStash.class);
                    Assert.verify(securityStash, ES_COMPLIANCE_000,"SecurityStash(认证信息)不存在");
                    switch (key){
                        case AUTHTOKEN_STASH_NAMING:
                            String authToken = securityStash.getAuthToken();
                            Assert.verify(authToken, ES_COMPLIANCE_000,"SecurityStash(令牌)不存在");
                            return authToken;
                        case PRINCIPAL_STASH_NAMING:
                            String principal = securityStash.getPrincipal();
                            Assert.verify(principal, ES_COMPLIANCE_000,"SecurityStash(用户信息)不存在");
                            return principal;
                        default:throw new EventException(ES_COMPLIANCE_000,"SecurityStash(枚举)访问错误");
                    }
                });
    }
}
