package org.lambda.framework.security.manger;


import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.lambda.framework.security.contract.SecurityContract;
import org.lambda.framework.security.enums.SecurityExceptionEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;


/**
 * @description: AuthToken认证管理器
 * @author: Mr.WangGang
 * @create: 2018-10-19 下午 1:18
 **/
public abstract class SecurityAutzManager implements ReactiveAuthorizationManager<AuthorizationContext>, SecurityAutzVerify {

    private SecurityAuthManager securityAuthManager;
    public SecurityAutzManager(SecurityAuthManager securityAuthManager){
        if(securityAuthManager == null)throw new EventException(SecurityExceptionEnum.ES_SECURITY_000);
        this.securityAuthManager = securityAuthManager;
    }

    @Resource(name = "securityAutzRedisOperation")
    private ReactiveRedisOperation securityAutzRedisOperation;

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext authorizationContext) {

         return securityAuthManager.authenticate(authorizationContext).flatMap(auth->{
            if(!auth.isAuthenticated()) return Mono.error(new EventException(SecurityExceptionEnum.ES_SECURITY_000));
            String currentPath = authorizationContext.getExchange().getRequest().getURI().getPath();
            // redis可能获取信息发生错误，导致直接抛出异常。所以默认空值，用于判断permit_all_url逻辑;
            return securityAutzRedisOperation.get(currentPath)
                        .onErrorResume(e->Mono.just(SecurityContract.LAMBDA_SECURITY_EMPTY_STR))
                        .defaultIfEmpty(SecurityContract.LAMBDA_SECURITY_EMPTY_STR)
                        .flatMap(currentPathAutzTree->{
                            if(!verify(currentPathAutzTree.toString(),auth.getPrincipal().toString()))return Mono.error(new EventException(SecurityExceptionEnum.ES_SECURITY_001));
                            return Mono.just(new AuthorizationDecision(true));
                    });
         });
    }
}
