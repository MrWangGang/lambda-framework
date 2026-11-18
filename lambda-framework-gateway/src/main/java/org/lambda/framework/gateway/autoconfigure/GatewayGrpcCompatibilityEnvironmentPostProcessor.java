package org.lambda.framework.gateway.autoconfigure;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;

import java.util.Map;

/**
 * 兼容 Qdrant 等引入了 io.grpc.Channel（如 grpc-netty-shaded）但缺少 unshaded grpc-netty 的场景：
 * 当 NettyChannelBuilder 不在类路径上时，默认关闭 Spring Cloud Gateway 的 JsonToGrpc 过滤器，
 * 避免 GatewayAutoConfiguration 初始化 GrpcSslConfigurer 时触发 NoClassDefFoundError。
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class GatewayGrpcCompatibilityEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String JSON_TO_GRPC_ENABLED = "spring.cloud.gateway.server.webflux.filter.json-to-grpc.enabled";
    private static final String PROPERTY_SOURCE_NAME = "lambdaGatewayGrpcCompatibility";
    private static final String GRPC_CHANNEL_CLASS = "io.grpc.Channel";
    private static final String UNSHADED_NETTY_BUILDER_CLASS = "io.grpc.netty.NettyChannelBuilder";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ClassLoader classLoader = application != null ? application.getClassLoader() : getClass().getClassLoader();
        if (ClassUtils.isPresent(GRPC_CHANNEL_CLASS, classLoader)
                && !ClassUtils.isPresent(UNSHADED_NETTY_BUILDER_CLASS, classLoader)
                && !environment.containsProperty(JSON_TO_GRPC_ENABLED)) {
            environment.getPropertySources().addLast(
                    new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of(JSON_TO_GRPC_ENABLED, Boolean.FALSE)));
        }
    }
}
