package org.lambda.framework.web.security.config;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.web.security.manger.SecurityAuthManager;
import org.lambda.framework.web.security.manger.SecurityAutzManager;
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

import static org.lambda.framework.common.enums.ExceptionEnum.EA00000000;
import static org.lambda.framework.common.enums.ExceptionEnum.EA00000001;


/**
 * @program: decisionsupportsystem
 * @description: spring权限框架配置
 * @author: Mr.WangGang
 * @create: 2018-08-29 17:08
 **/
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Value("#{'${lambda.security.permit_urls:}'.empty ?new String[]{''} :'${lambda.security.permit_urls:}'.split(',')}")
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
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,ReactiveAuthorizationManager autzManager) {
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
                throw new EventException(EA00000000);
            }
        });
        http.exceptionHandling().accessDeniedHandler(new ServerAccessDeniedHandler() {
            @Override
            public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
                throw new EventException(EA00000001);
            }
        });
        http.authorizeExchange().pathMatchers(permitUrls).permitAll();
        //http.addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        http.authorizeExchange().anyExchange().access(autzManager);
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityAutzManager securityAutzManager(SecurityAuthManager securityAuthManager){
        return new SecurityAutzManager(securityAuthManager) {
            @Override
            public boolean verify(String currentPathAutzTree, String principal) {
                return true;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityAuthManager securityAuthManager(){
        return new SecurityAuthManager(){
            @Override
            public boolean verify(String principal) {
                return true;
            }
        };
    }
}
