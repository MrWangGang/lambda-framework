package org.lambda.framework.mq.config.rabbitmq;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;
import org.lambda.framework.common.exception.Assert;
import org.springframework.context.annotation.Bean;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.*;

import java.util.ArrayList;
import java.util.List;

import static org.lambda.framework.mq.enums.MqExceptionEnum.*;

public abstract class AbstractReactiveRabbitMQConfig {
    protected abstract String host();
    protected abstract String user();
    protected abstract String password();
    protected  ConnectionFactory connectionFactory;
    protected  Sender sender;
    @Bean
    public ConnectionFactory connectionFactory() {
        Assert.verify(this.host(),ES_MQ_RABBITMQ_000);
        Assert.verify(this.user(),ES_MQ_RABBITMQ_001);
        Assert.verify(this.password(),ES_MQ_RABBITMQ_002);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.useNio();
        connectionFactory.setUsername(this.user());
        connectionFactory.setPassword(this.password());
        this.connectionFactory = connectionFactory;
        return connectionFactory;
    }

    @Bean
    public Sender buildSender(ConnectionFactory connectionFactory) {
        String[] hostPortStrings = this.host().split(",");
        List<Address> addressesList = new ArrayList<>();
        // 遍历分割后的每个 host:port 字符串
        for (String hostPortString : hostPortStrings) {
            String[] hosts = hostPortString.split(":");
            // 创建 Address 对象并加入列表
            Address address = new Address(hosts[0], Integer.parseInt(hosts[1]));
            addressesList.add(address);
        }
        Address[] addresses = addressesList.toArray(new Address[addressesList.size()]);
        Assert.verify(addresses,ES_MQ_RABBITMQ_000);
        SenderOptions senderOptions =  new SenderOptions()
                .connectionFactory(connectionFactory)
                .connectionSupplier(cf -> cf.newConnection(addresses,"Sender"))
                .resourceManagementScheduler(Schedulers.boundedElastic());

        this.sender = RabbitFlux.createSender(senderOptions);
        return sender;
    }
    @Bean
    public Receiver buildReceiver(ConnectionFactory connectionFactory) {
        String[] hostPortStrings = this.host().split(",");
        List<Address> addressesList = new ArrayList<>();
        // 遍历分割后的每个 host:port 字符串
        for (String hostPortString : hostPortStrings) {
            String[] hosts = hostPortString.split(":");
            // 创建 Address 对象并加入列表
            Address address = new Address(hosts[0], Integer.parseInt(hosts[1]));
            addressesList.add(address);
        }
        Address[] addresses = addressesList.toArray(new Address[0]);
        Assert.verify(addresses,ES_MQ_RABBITMQ_000);
        ReceiverOptions senderOptions =  new ReceiverOptions()
                .connectionFactory(connectionFactory)
                .connectionSupplier(cf -> cf.newConnection(addresses,"receiver"))
                .connectionSubscriptionScheduler(Schedulers.boundedElastic());
        return RabbitFlux.createReceiver(senderOptions);
    }







}
