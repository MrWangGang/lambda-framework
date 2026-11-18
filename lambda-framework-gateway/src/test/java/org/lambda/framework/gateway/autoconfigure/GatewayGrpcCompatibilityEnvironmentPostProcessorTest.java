package org.lambda.framework.gateway.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.GrpcSslConfigurer;
import org.springframework.cloud.gateway.filter.factory.JsonToGrpcGatewayFilterFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.http.codec.ServerCodecConfigurer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayGrpcCompatibilityEnvironmentPostProcessorTest {

    @Test
    void disablesJsonToGrpcWhenChannelPresentWithoutUnshadedNettyBuilder() {
        var processor = new GatewayGrpcCompatibilityEnvironmentPostProcessor();
        ConfigurableEnvironment env = new StandardEnvironment();

        processor.postProcessEnvironment(env, new SpringApplication(GatewayGrpcCompatibilityEnvironmentPostProcessorTest.class));

        assertThat(env.getProperty(GatewayGrpcCompatibilityEnvironmentPostProcessor.JSON_TO_GRPC_ENABLED, Boolean.class))
                .isFalse();
        assertThat(SpringFactoriesLoader.loadFactoryNames(
                org.springframework.boot.EnvironmentPostProcessor.class,
                getClass().getClassLoader()))
                .contains(GatewayGrpcCompatibilityEnvironmentPostProcessor.class.getName());
    }

    @Test
    void respectsExplicitJsonToGrpcConfiguration() {
        var processor = new GatewayGrpcCompatibilityEnvironmentPostProcessor();
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("explicit", Map.of(
                GatewayGrpcCompatibilityEnvironmentPostProcessor.JSON_TO_GRPC_ENABLED, "true")));

        processor.postProcessEnvironment(env, new SpringApplication(GatewayGrpcCompatibilityEnvironmentPostProcessorTest.class));

        assertThat(env.getProperty(GatewayGrpcCompatibilityEnvironmentPostProcessor.JSON_TO_GRPC_ENABLED, Boolean.class))
                .isTrue();
    }

    @Test
    void doesNotDisableWhenGrpcChannelIsAbsent() {
        var processor = new GatewayGrpcCompatibilityEnvironmentPostProcessor();
        ConfigurableEnvironment env = new StandardEnvironment();
        var app = new SpringApplication(GatewayGrpcCompatibilityEnvironmentPostProcessorTest.class);
        app.setResourceLoader(new org.springframework.core.io.DefaultResourceLoader(
                new FilteredClassLoader(io.grpc.Channel.class)));

        processor.postProcessEnvironment(env, app);

        assertThat(env.containsProperty(GatewayGrpcCompatibilityEnvironmentPostProcessor.JSON_TO_GRPC_ENABLED))
                .isFalse();
    }

    @Test
    void gatewayAutoConfigurationStartsCleanlyWithPostProcessorWhenOnlyShadedGrpcPresent() {
        var runner = new ReactiveWebApplicationContextRunner()
                .withInitializer(context -> new GatewayGrpcCompatibilityEnvironmentPostProcessor()
                        .postProcessEnvironment(context.getEnvironment(), null))
                .withBean(SslBundles.class, DefaultSslBundleRegistry::new)
                .withBean(ServerProperties.class, ServerProperties::new)
                .withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class, GatewayAutoConfiguration.class));

        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(GrpcSslConfigurer.class);
            assertThat(context).doesNotHaveBean(JsonToGrpcGatewayFilterFactory.class);
        });

        var baseline = new ReactiveWebApplicationContextRunner()
                .withBean(SslBundles.class, DefaultSslBundleRegistry::new)
                .withBean(ServerProperties.class, ServerProperties::new)
                .withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class, GatewayAutoConfiguration.class));

        baseline.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(ClassNotFoundException.class);
        });
    }
}
