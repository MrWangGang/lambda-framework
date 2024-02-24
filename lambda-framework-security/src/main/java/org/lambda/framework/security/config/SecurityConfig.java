package org.lambda.framework.security.config;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.security.enums.SecurityExceptionEnum;
import org.lambda.framework.security.manger.SecurityAuthManager;
import org.lambda.framework.security.manger.SecurityAutzManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;


/**
 * @program: decisionsupportsystem
 * @description: spring权限框架配置
 * @author: Mr.WangGang
 * @create: 2018-08-29 17:08
 **/
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Configuration
@SuppressWarnings("unchecked")
public class SecurityConfig {

    @Value("#{'${lambda.security.permit-urls:}'.empty ?new String[]{''} :'${lambda.security.permit-urls:}'.split(',')}")
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true); // 允许发送身份验证信息
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("OPTIONS");
        configuration.addAllowedMethod("HEAD");
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("PATCH");
        configuration.addAllowedHeader("User-Agent");
        configuration.addAllowedHeader("Cache-Control");
        configuration.addAllowedHeader("X-Requested-With");
        configuration.addAllowedHeader("Content-Type");
        configuration.addAllowedHeader("Accept");
        configuration.addAllowedHeader("Accept-Encoding");
        configuration.addAllowedHeader("Accept-Language");
        configuration.addAllowedHeader("Authorization");
        configuration.addAllowedHeader("Auth-Token");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 对所有URL生效
        return source;
    }
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,ReactiveAuthorizationManager autzManager) {
        http.httpBasic(e->e.disable());
        http.formLogin(e->e.disable());
        http.logout(e->e.disable());
        http.headers(e->e.disable());
        http.csrf(e->e.disable());
        http.requestCache(e->e.disable());
        http.cors(e->e.configurationSource(corsConfigurationSource())); // 启用CORS支持，并使用上述配置
        //禁用请求换成，禁用session
        http.requestCache(e->e.requestCache(NoOpServerRequestCache.getInstance()));
        http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        http.exceptionHandling(e->e.authenticationEntryPoint((a,b)->{throw new EventException(SecurityExceptionEnum.ES_SECURITY_000);}));
        http.exceptionHandling(e->e.accessDeniedHandler((a,b)->{throw new EventException(SecurityExceptionEnum.ES_SECURITY_001);}));
        http.authorizeExchange(e->e.pathMatchers(permitUrls).permitAll());
        //http.addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        http.authorizeExchange(e->e.anyExchange().access(autzManager));
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
