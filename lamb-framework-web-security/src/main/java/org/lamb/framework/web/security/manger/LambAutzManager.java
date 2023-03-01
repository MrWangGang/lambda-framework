package org.lamb.framework.web.security.manger;


import org.apache.commons.lang3.StringUtils;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.redis.operation.LambReactiveRedisOperation;
import org.lamb.framework.web.security.contract.Contract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;


/**
 * @description: AuthToken认证管理器
 * @author: Mr.WangGang
 * @create: 2018-10-19 下午 1:18
 **/
public abstract class LambAutzManager implements ReactiveAuthorizationManager<AuthorizationContext>,LambSecurityAutzVerify {

    private LambAuthManager lambAuthManager;
    public LambAutzManager(LambAuthManager lambAuthManager){
        if(lambAuthManager == null)throw new LambEventException(EA00000000);
        this.lambAuthManager = lambAuthManager;
    }

    @Resource(name = "lambAutzRedisTemplate")
    private ReactiveRedisTemplate lambAutzRedisTemplate;


    private String urlAutz;
    @Value("${lamb.security.url_autz:"+ Contract.URL_AUTZ_ALL+"}")
    public void setUrlAutz(String urlAutz) {
        if(Contract.URL_AUTZ_ALL.equals(urlAutz) || Contract.URL_AUTZ_MAPPING.equals(urlAutz)) {
            this.urlAutz = urlAutz;
        }else {
            throw new LambEventException(EA00000010);
        }
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext authorizationContext) {

         return lambAuthManager.authenticate(authorizationContext).flatMap(auth->{
            if(!auth.isAuthenticated()) return Mono.error(new LambEventException(EA00000000));
            String currentPath = authorizationContext.getExchange().getRequest().getURI().getPath();
            // redis可能获取信息发生错误，导致直接抛出异常。所以默认空值，用于判断permit_all_url逻辑;
            return LambReactiveRedisOperation.build(lambAutzRedisTemplate).get(currentPath)
                        .onErrorResume(e->Mono.just(Contract.EMPTY_STR))
                        .defaultIfEmpty(Contract.EMPTY_STR)
                        .flatMap(currentPathAutzTree->{
                            if(StringUtils.isBlank(currentPathAutzTree.toString())){
                                //如果路径权限树为空
                                if(Contract.URL_AUTZ_ALL.equals(urlAutz)){
                                    //配置了所有的经过认证都需要授权
                                    return Mono.error(new LambEventException(EA00000001));
                                }
                                if(Contract.URL_AUTZ_MAPPING.equals(urlAutz)){
                                    //配置了只有映射的URL经过认证才需要授权
                                    return Mono.just(currentPathAutzTree.toString());
                                }
                                return Mono.error(new LambEventException(EA00000010));
                            }
                            return Mono.just(currentPathAutzTree.toString());
                        }).flatMap(currentPathAutzTree->{
                            if(!verify(currentPathAutzTree,auth.getPrincipal().toString()))return Mono.error(new LambEventException(EA00000001));
                            return Mono.just(new AuthorizationDecision(true));
                    });
         });
    }
}
