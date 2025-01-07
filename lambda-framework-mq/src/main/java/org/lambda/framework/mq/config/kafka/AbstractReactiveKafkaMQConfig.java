package org.lambda.framework.mq.config.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractReactiveKafkaMQConfig {
    protected abstract String host();

    @Bean
    public KafkaMQReactiveOperate kafkaMqOperate(){
        return new KafkaMQReactiveOperate();
    }

    @Bean
    public KafkaSender<String, String> KafkaSender() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, host());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        SenderOptions<String, String> options = SenderOptions.create(props);
        return KafkaSender.create(options);
    }

    @Bean
    public KafkaReceiverFactory KafkaReceiver() {
        return new KafkaReceiverFactory(this.host());
    }


    public static class KafkaReceiverFactory {
        private final String host;

        public KafkaReceiverFactory(String host){
            this.host = host;
        }

        public KafkaReceiver<String, String> createReceiver(String topic,String groupId) {
            // 创建基础 ReceiverOptions 配置，暂不绑定消费组 ID 和主题
            Map<String, Object> consumerProps = new HashMap<>();
            consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.host);
            consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            // 指定消费组ID
            consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            // 创建ReceiverOptions对象
            ReceiverOptions<String, String> options = ReceiverOptions.create(consumerProps);
            // 指定要订阅的主题
            options = options.subscription(Collections.singleton(topic));
            return KafkaReceiver.create(options);
        }
    }
}
