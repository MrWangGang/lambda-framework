package org.lambda.framework.mq.config.rabbitmq;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.startup.ComponentCheck;
import org.lambda.framework.common.startup.DeploymentMode;
import org.lambda.framework.enums.MqExceptionEnum;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

@AutoConfiguration
@ConditionalOnProperty("lambda.mq.rabbitmq.addresses")
@EnableConfigurationProperties(RabbitMqAutoConfiguration.Settings.class)
public class RabbitMqAutoConfiguration {
    @ConfigurationProperties("lambda.mq.rabbitmq")
    record Settings(@DefaultValue("single") DeploymentMode mode, String addresses,
                    String username, String password, @DefaultValue("/") String virtualHost,
                    @DefaultValue("false") boolean tls) {
        Address[] nodes() {
            Address[] nodes = Address.parseAddresses(addresses);
            long unique = java.util.Arrays.stream(nodes).distinct().count();
            if (mode == DeploymentMode.SINGLE ? unique != 1 : unique < 2) {
                throw new EventException(MqExceptionEnum.CONFIGURATION_INVALID, "RabbitMQ single requires one address; cluster requires multiple addresses");
            }
            return nodes;
        }
    }

    @Bean
    @ConditionalOnMissingBean
    ConnectionFactory connectionFactory(Settings settings) throws java.security.GeneralSecurityException {
        settings.nodes();
        var factory = new ConnectionFactory();
        factory.useNio();
        if (settings.username() != null) {
            factory.setUsername(settings.username());
        }
        if (settings.password() != null) {
            factory.setPassword(settings.password());
        }
        factory.setVirtualHost(settings.virtualHost());
        factory.setConnectionTimeout(5000);
        if (settings.tls()) {
            factory.useSslProtocol();
            factory.enableHostnameVerification();
        }
        return factory;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    Sender rabbitSender(ConnectionFactory factory, Settings settings) {
        var nodes = settings.nodes();
        return RabbitFlux.createSender(new SenderOptions().connectionFactory(factory)
                .connectionSupplier(client -> client.newConnection(nodes, "lambda-sender"))
                .resourceManagementScheduler(Schedulers.boundedElastic()));
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    Receiver rabbitReceiver(ConnectionFactory factory, Settings settings) {
        var nodes = settings.nodes();
        return RabbitFlux.createReceiver(new ReceiverOptions().connectionFactory(factory)
                .connectionSupplier(client -> client.newConnection(nodes, "lambda-receiver"))
                .connectionSubscriptionScheduler(Schedulers.boundedElastic()));
    }

    @Bean
    @ConditionalOnMissingBean
    RabbitMQReactiveOperate rabbitMqOperate() {
        return new RabbitMQReactiveOperate();
    }

    @Bean
    ComponentCheck rabbitCheck(ConnectionFactory factory, Settings settings) {
        return new ComponentCheck("RabbitMQ", () -> Mono.fromRunnable(() -> {
            try (var connection = factory.newConnection(settings.nodes(), "lambda-startup-check");
                 var channel = connection.createChannel()) {
                if (!channel.isOpen()) {
                    throw new EventException(MqExceptionEnum.CONNECTION_FAILED, "RabbitMQ channel is closed");
                }
            } catch (Exception error) {
                throw new EventException(MqExceptionEnum.CONNECTION_FAILED, "RabbitMQ connection failed", error);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then());
    }
}
