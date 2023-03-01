package org.lamb.framework.web.security.config;

import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.web.security.manger.LambAuthManager;
import org.lamb.framework.web.security.manger.LambAutzManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.lamb.framework.common.enums.LambExceptionEnum.EA00000000;
import static org.lamb.framework.common.enums.LambExceptionEnum.EA00000001;


/**
 * @program: decisionsupportsystem
 * @description: spring权限框架配置
 * @author: Mr.WangGang
 * @create: 2018-08-29 17:08
 **/
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class LambSecurityConfig {

    @Value("#{'${lamb.security.permit_urls:}'.empty ?new String[]{''} :'${lamb.security.permit_urls:}'.split(',')}")
    private String[] permitUrls;


    //@Bean
    //public AuthenticationWebFilter authenticationWebFilter(){
    //    AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(new ReactiveAuthenticationManager() {
    //        @Override
    //        public Mono<Authentication> authenticate(Authentication authentication) {
    //            authentication.setAuthenticated(true);
    //            return Mono.just(authentication);
    //        }
    //    });
    //
    //    authenticationWebFilter.setRequiresAuthenticationMatcher(e-> {
    //            final AntPathMatcher antPathMatcher = new AntPathMatcher();
    //            for (String url : permitUrls) {
    //                //当前请求和忽略url匹配
    //                if (antPathMatcher.match(url, e.getRequest().getURI().getPath())) {
    //                    //返回false 放过请求
    //                    return ServerWebExchangeMatcher.MatchResult.notMatch();
    //                }
    //            }
    //            //不在忽略url中，经过认证处理器
    //            return ServerWebExchangeMatcher.MatchResult.match();
    //    });
    //    return authenticationWebFilter;
    //}

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,ReactiveAuthorizationManager lambAutzManager) {
        http.httpBasic().disable();
        http.formLogin().disable();
        http.logout().disable();
        http.headers().disable();
        http.csrf().disable();
        http.requestCache().disable();
        //禁用请求换成，禁用session
        http.requestCache().requestCache(NoOpServerRequestCache.getInstance());
        http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        http.exceptionHandling().authenticationEntryPoint(new ServerAuthenticationEntryPoint() {
            @Override
            public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
                throw new LambEventException(EA00000000);
            }
        });
        http.exceptionHandling().accessDeniedHandler(new ServerAccessDeniedHandler() {
            @Override
            public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
                throw new LambEventException(EA00000001);
            }
        });
        http.authorizeExchange().pathMatchers(permitUrls).permitAll();
        //http.addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        http.authorizeExchange().anyExchange().access(lambAutzManager);
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public LambAutzManager lambAutzManager(LambAuthManager lambAuthManager){
        return new LambAutzManager(lambAuthManager) {
            @Override
            public boolean verify(String currentPathAutzTree, String principal) {
                return true;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public LambAuthManager lambAuthManager(){
        return new LambAuthManager(){
            @Override
            public boolean verify(String principal) {
                return true;
            }
        };
    }
}
