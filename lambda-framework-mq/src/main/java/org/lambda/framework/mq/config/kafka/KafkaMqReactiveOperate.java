package org.lambda.framework.mq.config.kafka;

import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.lambda.framework.common.exception.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import static org.lambda.framework.mq.enums.MqExceptionEnum.*;

public class KafkaMqReactiveOperate {

    @Resource
    private KafkaSender<String, String> kafkaSender;

    @Resource
    private AbstractReactiveKafkaMqConfig.KafkaReceiverFactory kafkaReceiverFactory;

    private Mono<Void> send(Declare declare,String key,String message) {
        Assert.verify(key,ES_MQ_KAFKA_003);
        return this.sendMessage(declare,key,message);
    }

    private Mono<Void> send(Declare declare,String message) {
        return this.sendMessage(declare,null,message);
    }

    private Mono<Void> sendMessage(Declare declare, String key,String message) {
        Assert.check(declare,ES_MQ_KAFKA_001);
        Assert.verify(message,ES_MQ_KAFKA_002);
        return kafkaSender.send(Mono.just(
                        SenderRecord.create(declare.getTopic(), null, null, key, message, null)))
                .then();
    }

    public Flux<ReceiverRecord<String, String>> receiverManual(Declare declare) {
        Assert.check(declare,ES_MQ_KAFKA_001);
        return kafkaReceiverFactory.createReceiver(declare.getTopic(), declare.getGroupId()).receive();
    }

    public Flux<Flux<ConsumerRecord<String, String>>> receiverAuto(Declare declare){
        Assert.check(declare,ES_MQ_KAFKA_001);
        return kafkaReceiverFactory.createReceiver(declare.getTopic(), declare.getGroupId()).receiveAutoAck();
    }



    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Declare{
        //主题
        private String topic;
        //消费组id
        private String groupId;
    }
}
