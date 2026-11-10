package org.lambda.framework.security.config;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.SecurityStash;
import org.lambda.framework.common.templete.ResponseTemplete;
import org.lambda.framework.common.util.JsonUtil;
import org.lambda.framework.security.enums.SecurityExceptionEnum;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import org.lambda.framework.compliance.security.PrincipalFactory;
import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableWebFluxSecurity
@EnableConfigurationProperties(SecurityConfig.Settings.class)
public class SecurityConfig {
    @ConfigurationProperties("lambda.security")
    record Settings(@DefaultValue List<String> permitPaths) {}

    @Bean
    @ConditionalOnMissingBean
    SecurityWebFilterChain lambdaSecurityFilterChain(ServerHttpSecurity http, PrincipalFactory principals,
            Settings settings, ReactiveRedisOperation redis) {
        var paths = settings.permitPaths().stream().map(PathPatternParser.defaultInstance::parse).toList();
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .addFilterAt((exchange, chain) -> authenticate(exchange, chain, principals, paths),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(rules -> rules.anyExchange().permitAll())
                .build();
    }

    private Mono<Void> authenticate(ServerWebExchange exchange, WebFilterChain chain, PrincipalFactory principals,
            List<PathPattern> paths) {
        var path = exchange.getRequest().getPath().pathWithinApplication();
        if (paths.stream().anyMatch(pattern -> pattern.matches(path))) {
            return chain.filter(exchange);
        }
        String token = requestToken(exchange.getRequest().getHeaders());
        if (token == null || token.length() > 1024) {
            return unauthorized(exchange);
        }
        return principals.resolvePrincipal(token).map(Optional::of)
                .onErrorReturn(EventException.class, Optional.empty())
                .defaultIfEmpty(Optional.empty()).flatMap(principal -> {
                    if (principal.isEmpty()) {
                        return unauthorized(exchange);
                    }
                    String subject = principal.get();
                    var stash = new SecurityStash(token, subject);
                    var authentication = UsernamePasswordAuthenticationToken.authenticated(subject, null, List.of());
                    return chain.filter(exchange).contextWrite(context -> context.put(SecurityStash.class, stash))
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                });
    }

    private static String requestToken(HttpHeaders headers) {
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7);
        }
        return headers.getFirst("Auth-Token");
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        var response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var body = new ResponseTemplete();
        var error = SecurityExceptionEnum.ES_SECURITY_000;
        body.setServiceCode(error.getCode());
        body.setServiceMessage(error.getMessage());
        return response.writeWith(Mono.fromSupplier(() ->
                response.bufferFactory().wrap(JsonUtil.write(body).getBytes(StandardCharsets.UTF_8))));
    }
}
