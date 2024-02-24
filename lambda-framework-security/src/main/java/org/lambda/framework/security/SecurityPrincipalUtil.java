package org.lambda.framework.security;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.compliance.security.PrincipalUtil;
import org.lambda.framework.compliance.security.container.LambdaSecurityAuthToken;
import org.lambda.framework.compliance.security.container.SecurityContract;
import org.lambda.framework.compliance.security.container.SecurityLoginUser;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.*;
import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.ES_COMPLIANCE_021;
import static org.lambda.framework.compliance.security.container.SecurityContract.TOKEN_SUFFIX;

@Component
public class SecurityPrincipalUtil extends PrincipalUtil {


    public Mono<String> getPrincipal() {
        return this.getServerRequestToken().flatMap(reqKey->{
            return this.getSecurityAuthTokenKey(reqKey).flatMap(key->{
                return this.getSecurityAuthToken(key).flatMap(securityAuthToken->{
                    return Mono.just(securityAuthToken.getPrincipal());
                });
            });
        });
    }
    public <T extends SecurityLoginUser<?>>Mono<T> getPrincipal2Object(Class<T> clazz) {
        return this.getPrincipal().flatMap(e -> {
            return Mono.just(JsonUtil.stringToObj(e,clazz).orElseThrow(()->new EventException(ES_COMPLIANCE_023)));
        }).switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_019)));
    }

}
