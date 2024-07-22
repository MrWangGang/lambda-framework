package org.lambda.framework.mq.config.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.*;

import java.util.HashMap;
import java.util.Map;

import static org.lambda.framework.mq.enums.MqExceptionEnum.*;
@Component
public class RabbitMqReactiveOperate {

    @Resource
    private ConnectionFactory connectionFactory;
    @Resource
    private Sender sender;
    @Resource
    private Receiver receiver;

    public Mono<Void> sendDelay(Declare declare, Long delayMillis,String message) {
        return this.sendDelay(declare,delayMillis,message,new HashMap<>());
    }


    public Mono<Void> sendDelay(Declare declare, Long delayMillis,String message,Map<String, Object> headers){
        Assert.check(declare,ES_MQ_RABBITMQ_004);
        Assert.verify(delayMillis,ES_MQ_RABBITMQ_005);
        Assert.verify(message,ES_MQ_RABBITMQ_007);
        Assert.verify(headers,ES_MQ_RABBITMQ_008);
        //声明主队列交换机
        // 依次声明交换机、队列，并绑定

        Flux<OutboundMessage> messages = Flux.defer(()->{
            // 设置消息的延迟时间
            Map<String, Object> mutableHeaders = new HashMap<>(headers);

            mutableHeaders.put("x-delay", delayMillis);
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .headers(mutableHeaders)
                    .build();

            // 创建 OutboundMessage 对象
            OutboundMessage outboundMessage = new OutboundMessage(
                    declare.getExchangeName(),
                    declare.getRoutingKey(),
                    props,
                    message.getBytes()
            );
            // 返回包含单个 OutboundMessage 的 Flux
            return Flux.just(outboundMessage);
        });

        Flux<OutboundMessageResult> resultFlux = sender.sendWithPublishConfirms(messages);
        return resultFlux.flatMap(ack->{
            if(ack.isAck()){
                return Mono.empty();
            }
            throw new EventException(ES_MQ_000);
        }).then();
    }



    public Flux<AcknowledgableDelivery> receiverManual(String queue){
        Flux<AcknowledgableDelivery> deliveryFlux = receiver.consumeManualAck(queue);
        return deliveryFlux;
    }

    public Flux<Delivery> receiverAuto(String queue){
        Flux<Delivery> deliveryFlux = receiver.consumeAutoAck(queue);
        return deliveryFlux;
    }


    public Mono<AMQP.Queue.BindOk> declareDelay(Declare declare){
        // 声明主交换机
        ExchangeSpecification primaryExchangeSpec = new ExchangeSpecification()
                .type("x-delayed-message") // 主交换机的类型可以根据需求设置
                .name(declare.getExchangeName())
                .durable(true)
                .autoDelete(false)
                .arguments(Map.of("x-delayed-type", "direct"));

        // 声明死信交换机
        ExchangeSpecification dlxExchangeSpec = new ExchangeSpecification()
                .type("direct") // 死信交换机的类型可以根据需求设置
                .name(declare.getDlxExchangeName())
                .durable(true)
                .autoDelete(false);

        // 声明主队列
        QueueSpecification primaryQueueSpec = new QueueSpecification()
                .name(declare.getQueueName())
                .durable(true)
                .autoDelete(false)
                .arguments(Map.of(
                        "x-dead-letter-exchange", declare.getDlxExchangeName(),
                        "x-dead-letter-routing-key", declare.getDlxRoutingKey()
                ));

        // 声明死信队列
        QueueSpecification dlxQueueSpec = new QueueSpecification()
                .name(declare.getDlxQueueName())
                .durable(true)
                .autoDelete(false);

        // 绑定主队列到主交换机
        BindingSpecification primaryBindingSpec = BindingSpecification.binding()
                .exchange(declare.getExchangeName())
                .queue(declare.getQueueName())
                .routingKey(declare.getRoutingKey());

        // 绑定死信队列到死信交换机
        BindingSpecification dlxBindingSpec = BindingSpecification.binding()
                .exchange(declare.getDlxExchangeName())
                .queue(declare.getDlxQueueName())
                .routingKey(declare.getDlxRoutingKey());

        // 依次声明交换机、队列，并绑定
        return sender.declare(primaryExchangeSpec)
                .then(sender.declare(dlxExchangeSpec))
                .then(sender.declare(primaryQueueSpec))
                .then(sender.declare(dlxQueueSpec))
                .then(sender.bind(primaryBindingSpec))
                .then(sender.bind(dlxBindingSpec));
    }


    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Declare{
        //路由键
        private String routingKey;
        //死信路由键
        private String dlxRoutingKey;
        //名称
        private String exchangeName;
        //死信交换机
        private String dlxExchangeName;
        //名称
        private String queueName;
        //死信队列名称
        private String dlxQueueName;
    }

}
