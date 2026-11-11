package org.lambda.framework.mq.config.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.startup.ComponentCheck;
import org.lambda.framework.common.startup.DeploymentMode;
import org.lambda.framework.enums.MqExceptionEnum;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@ConditionalOnProperty("lambda.mq.kafka.bootstrap-servers")
@EnableConfigurationProperties(KafkaMqAutoConfiguration.Settings.class)
public class KafkaMqAutoConfiguration {
    @ConfigurationProperties("lambda.mq.kafka")
    record Settings(String bootstrapServers, @DefaultValue("single") DeploymentMode mode) {
        Settings {
            long count = java.util.Arrays.stream(bootstrapServers.split(","))
                    .map(String::strip).filter(server -> !server.isEmpty()).distinct().count();
            if (mode == DeploymentMode.SINGLE ? count != 1 : count < 2) {
                throw new EventException(MqExceptionEnum.CONFIGURATION_INVALID, "Kafka single requires one address; cluster requires multiple addresses");
            }
        }
    }

    private final Settings settings;

    public KafkaMqAutoConfiguration(Settings settings) {
        this.settings = settings;
    }

    @Bean
    ComponentCheck kafkaCheck() {
        return new ComponentCheck("Kafka", () -> Mono.<Integer>create(sink -> {
            var admin = org.apache.kafka.clients.admin.Admin.create(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, settings.bootstrapServers()));
            admin.describeCluster().nodes().whenComplete((nodes, error) -> {
                admin.close(java.time.Duration.ZERO);
                if (error == null) {
                    sink.success(nodes.size());
                } else {
                    sink.error(new EventException(MqExceptionEnum.CONNECTION_FAILED, null, error));
                }
            });
            sink.onCancel(() -> admin.close(java.time.Duration.ZERO));
        }).flatMap(nodes -> nodes >= (settings.mode() == DeploymentMode.CLUSTER ? 2 : 1)
                ? Mono.empty() : Mono.error(new EventException(MqExceptionEnum.CONFIGURATION_INVALID, "Kafka broker topology does not match mode"))));
    }

    @Bean
    public KafkaMQReactiveOperate kafkaMqOperate(){
        return new KafkaMQReactiveOperate();
    }

    @Bean(destroyMethod = "close")
    public KafkaSender<String, String> KafkaSender() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, settings.bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        SenderOptions<String, String> options = SenderOptions.create(props);
        return KafkaSender.create(options);
    }

    @Bean
    public KafkaReceiverFactory KafkaReceiver() {
        return new KafkaReceiverFactory(this.settings.bootstrapServers());
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
