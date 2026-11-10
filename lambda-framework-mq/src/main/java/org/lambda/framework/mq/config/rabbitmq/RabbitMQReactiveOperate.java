package org.lambda.framework.mq.config.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Delivery;
import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.mq.RabbitMQDeclare;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.BindingSpecification;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.lambda.framework.mq.enums.MqExceptionEnum.ES_MQ_000;

public class RabbitMQReactiveOperate {
    @Resource
    private Sender sender;

    @Resource
    private Receiver receiver;

    public Mono<Void> sendDelay(RabbitMQDeclare declaration, Long delayMillis, String message) {
        return sendDelay(declaration, delayMillis, message, Map.of());
    }

    public Mono<Void> sendDelay(RabbitMQDeclare declaration, Long delayMillis, String message,
            Map<String, Object> headers) {
        return Mono.defer(() -> {
            Objects.requireNonNull(declaration, "RabbitMQ declaration");
            Objects.requireNonNull(message, "RabbitMQ message");
            if (delayMillis == null || delayMillis < 0 || delayMillis > 4_294_967_295L) {
                return Mono.error(new IllegalArgumentException("RabbitMQ delay must be 0..4294967295 milliseconds"));
            }
            var messageHeaders = new HashMap<>(headers);
            messageHeaders.put("x-delay", delayMillis);
            var properties = new AMQP.BasicProperties.Builder().deliveryMode(2).contentType("application/json")
                    .headers(messageHeaders).build();
            var outbound = new OutboundMessage(declaration.getExchangeName(), declaration.getRoutingKey(),
                    properties, message.getBytes(StandardCharsets.UTF_8));
            return sender.sendWithPublishConfirms(Mono.just(outbound)).single().flatMap(result ->
                    result.isAck() ? Mono.empty() : Mono.error(new EventException(ES_MQ_000)));
        });
    }

    public Flux<AcknowledgableDelivery> receiverManual(String queue) {
        return receiver.consumeManualAck(queue);
    }

    public Flux<Delivery> receiverAuto(String queue) {
        return receiver.consumeAutoAck(queue);
    }

    public Mono<AMQP.Queue.BindOk> declareDelay(RabbitMQDeclare declaration) {
        return declareDelay(declaration, Map.of());
    }

    public Mono<AMQP.Queue.BindOk> declareDelay(RabbitMQDeclare declaration, Map<String, Object> queueArguments) {
        var exchange = ExchangeSpecification.exchange(declaration.getExchangeName()).type("x-delayed-message")
                .durable(true).arguments(Map.of("x-delayed-type", "direct"));
        var queue = QueueSpecification.queue(declaration.getQueueName()).durable(true).arguments(queueArguments);
        var binding = BindingSpecification.binding(declaration.getExchangeName(), declaration.getRoutingKey(),
                declaration.getQueueName());
        return sender.declareExchange(exchange).then(sender.declareQueue(queue)).then(sender.bind(binding));
    }
}
