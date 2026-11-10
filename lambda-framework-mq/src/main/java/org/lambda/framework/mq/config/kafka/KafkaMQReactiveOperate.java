package org.lambda.framework.mq.config.kafka;

import jakarta.annotation.Resource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.mq.KafkaMQDeclare;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import static org.lambda.framework.enums.MqExceptionEnum.*;

public class KafkaMQReactiveOperate {

    @Resource
    private KafkaSender<String, String> kafkaSender;

    @Resource
    private KafkaMqAutoConfiguration.KafkaReceiverFactory kafkaReceiverFactory;

    public Mono<Void> send(KafkaMQDeclare declare, String key, String message) {
        Assert.verify(key,KAFKA_KEY_REQUIRED);
        return this.sendMessage(declare,key,message);
    }

    public Mono<Void> send(KafkaMQDeclare declare,String message) {
        return this.sendMessage(declare,null,message);
    }

    private Mono<Void> sendMessage(KafkaMQDeclare declare, String key,String message) {
        Assert.check(declare,KAFKA_DESTINATION_REQUIRED);
        Assert.verify(message,KAFKA_MESSAGE_REQUIRED);
        return kafkaSender.send(Mono.just(
                        SenderRecord.create(declare.getTopic(), null, null, key, message, null)))
                .then();
    }

    public Flux<ReceiverRecord<String, String>> receiverManual(KafkaMQDeclare declare) {
        Assert.check(declare,KAFKA_DESTINATION_REQUIRED);
        return kafkaReceiverFactory.createReceiver(declare.getTopic(), declare.getGroupId()).receive();
    }

    public Flux<Flux<ConsumerRecord<String, String>>> receiverAuto(KafkaMQDeclare declare){
        Assert.check(declare,KAFKA_DESTINATION_REQUIRED);
        return kafkaReceiverFactory.createReceiver(declare.getTopic(), declare.getGroupId()).receiveAutoAck();
    }

}
