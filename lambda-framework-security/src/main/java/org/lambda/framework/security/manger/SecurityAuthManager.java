package org.lambda.framework.security.manger;


import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.lambda.framework.security.container.SecurityAuthToken;
import org.lambda.framework.security.contract.SecurityContract;
import org.lambda.framework.security.enums.SecurityExceptionEnum;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;


/**
 * @description: AuthToken认证管理器
 * @author: Mr.WangGang
 * @create: 2021-10-19 下午 1:18
 **/
public abstract class SecurityAuthManager implements SecurityAuthVerify {
    @Resource(name = "securityAuthRedisTemplate")
    private ReactiveRedisTemplate securityAuthRedisTemplate;

    @Override
    public Mono<Authentication> authenticate(AuthorizationContext authorizationContext) {
        //converter
        ServerWebExchange exchange = authorizationContext.getExchange();
        String authToken = exchange.getRequest().getHeaders().getFirst(SecurityContract.AUTH_TOKEN_NAMING);
        if(StringUtils.isBlank(authToken))throw new EventException(SecurityExceptionEnum.ES_SECURITY_003);
        if(!(Pattern.compile(SecurityContract.LAMBDA_SECURITY_AUTH_TOKEN_REGX).matcher(authToken).matches()))throw new EventException(SecurityExceptionEnum.ES_SECURITY_007);
        //authentication
        //令牌与库中不匹配
        return ReactiveRedisOperation.build(securityAuthRedisTemplate).hasKey(authToken).onErrorResume(e1->Mono.error(new EventException(SecurityExceptionEnum.ES_SECURITY_003))).flatMap(e -> {
            if (e == null || !e) return Mono.error(new EventException(SecurityExceptionEnum.ES_SECURITY_003));
            return Mono.just(true);
        }).flatMap(e -> {
            //添加默认值防止nullpoint
            return ReactiveRedisOperation.build(securityAuthRedisTemplate).get(authToken).onErrorResume(e1->Mono.error(new EventException(SecurityExceptionEnum.ES_SECURITY_004)));
        }).flatMap(principal -> {
            if (principal == null )return Mono.error(new EventException(SecurityExceptionEnum.ES_SECURITY_004));
            if(StringUtils.isBlank(principal.toString()))return Mono.error(new EventException(SecurityExceptionEnum.ES_SECURITY_004));
            //刷新TOKEN存活时间 保持登陆
            //更新SecurityContext中的Authentication信息
            if(!verify(principal.toString())) return Mono.error(new EventException(SecurityExceptionEnum.ES_SECURITY_000));
            ReactiveRedisOperation.build(securityAuthRedisTemplate).expire(authToken, SecurityContract.LAMBDA_SECURITY_TOKEN_TIME_SECOND);
            return Mono.just(SecurityAuthToken.builder().principal(principal.toString()).credentials(authToken).authenticated(true).build());
       });
    }
}
