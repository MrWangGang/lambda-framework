package org.lambda.framework.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RabbitMQDeclare {
    //路由键
    private String routingKey;
    //名称
    private String exchangeName;
    //名称
    private String queueName;
}
