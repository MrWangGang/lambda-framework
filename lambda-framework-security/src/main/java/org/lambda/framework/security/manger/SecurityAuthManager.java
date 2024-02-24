package org.lambda.framework.security.manger;


import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.security.container.SecurityAuthToken;
import org.lambda.framework.compliance.security.container.SecurityContract;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.lambda.framework.security.SecurityPrincipalUtil;
import org.lambda.framework.security.enums.SecurityExceptionEnum;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.lambda.framework.compliance.security.container.SecurityContract.LAMBDA_SECURITY_TOKEN_TIME_SECOND;
import static org.lambda.framework.security.enums.SecurityExceptionEnum.ES_SECURITY_000;
import static org.lambda.framework.security.enums.SecurityExceptionEnum.ES_SECURITY_004;


/**
 * @description: AuthToken认证管理器
 * @author: Mr.WangGang
 * @create: 2021-10-19 下午 1:18
 **/
public abstract class SecurityAuthManager implements SecurityAuthVerify {
    @Resource(name = "securityAuthRedisOperation")
    private ReactiveRedisOperation securityAuthRedisOperation;

    @Resource
    private SecurityPrincipalUtil securityPrincipalUtil;

    @Override
    public Mono<SecurityAuthToken> authenticate(AuthorizationContext authorizationContext) {
        //converter
        ServerWebExchange exchange = authorizationContext.getExchange();
        String authToken = exchange.getRequest().getHeaders().getFirst(SecurityContract.AUTH_TOKEN_NAMING);
        if(StringUtils.isBlank(authToken))throw new EventException(SecurityExceptionEnum.ES_SECURITY_003);
        //authentication
        //令牌与库中不匹配
        return securityPrincipalUtil.getPrincipal().flatMap(principal -> {
            if(StringUtils.isBlank(principal.toString()))return Mono.error(new EventException(ES_SECURITY_004));
            //刷新TOKEN存活时间 保持登陆
            //更新SecurityContext中的Authentication信息
            if(!verify(principal)) return Mono.error(new EventException(ES_SECURITY_000));
            return securityAuthRedisOperation.expire(authToken, LAMBDA_SECURITY_TOKEN_TIME_SECOND)
                    .then(Mono.just(SecurityAuthToken.builder().principal(principal.toString()).credentials(authToken).authenticated(true).build()));
       }).switchIfEmpty(Mono.error(new EventException(ES_SECURITY_004)));
    }
}
