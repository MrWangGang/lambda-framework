package org.lamb.framework.web.security.manger;


import org.apache.commons.lang3.StringUtils;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.redis.operation.LambReactiveRedisOperation;
import org.lamb.framework.web.security.container.LambAuthToken;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.regex.Pattern;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;
import static org.lamb.framework.common.enums.LambSecurityNamingEnum.AUTH_TOKEN_NAMING;
import static org.lamb.framework.web.security.contract.LambSecurityContract.LAMB_SECURITY_AUTH_TOKEN_REGX;
import static org.lamb.framework.web.security.contract.LambSecurityContract.LAMB_SECURITY_TOKEN_TIME_SECOND;


/**
 * @description: AuthToken认证管理器
 * @author: Mr.WangGang
 * @create: 2021-10-19 下午 1:18
 **/
public abstract class LambAuthManager implements LambSecurityAuthVerify {
    @Resource(name = "lambAuthRedisTemplate")
    private ReactiveRedisTemplate lambAuthRedisTemplate;

    @Override
    public Mono<Authentication> authenticate(AuthorizationContext authorizationContext) {
        //converter
        ServerWebExchange exchange = authorizationContext.getExchange();
        String authToken = exchange.getRequest().getHeaders().getFirst(AUTH_TOKEN_NAMING);
        if(StringUtils.isBlank(authToken))throw new LambEventException(EA00000003);
        if(!(Pattern.compile(LAMB_SECURITY_AUTH_TOKEN_REGX).matcher(authToken).matches()))throw new LambEventException(EA00000007);
        //authentication
        //令牌与库中不匹配
        return LambReactiveRedisOperation.build(lambAuthRedisTemplate).hasKey(authToken).onErrorResume(e1->Mono.error(new LambEventException(EA00000003))).flatMap(e -> {
            if (e == null || !e) return Mono.error(new LambEventException(EA00000003));
            return Mono.just(true);
        }).flatMap(e -> {
            //添加默认值防止nullpoint
            return LambReactiveRedisOperation.build(lambAuthRedisTemplate).get(authToken).onErrorResume(e1->Mono.error(new LambEventException(EA00000004)));
        }).flatMap(principal -> {
            if (principal == null )return Mono.error(new LambEventException(EA00000004));
            if(StringUtils.isBlank(principal.toString()))return Mono.error(new LambEventException(EA00000004));
            //刷新TOKEN存活时间 保持登陆
            //更新SecurityContext中的Authentication信息
            if(!verify(principal.toString())) return Mono.error(new LambEventException(EA00000000));
            LambReactiveRedisOperation.build(lambAuthRedisTemplate).expire(authToken, LAMB_SECURITY_TOKEN_TIME_SECOND);
            return Mono.just(LambAuthToken.builder().principal(principal.toString()).credentials(authToken).authenticated(true).build());
       });
    }
}
