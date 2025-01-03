package org.lambda.framework.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KafkaMQDeclare {
    //主题
    private String topic;
    //消费组id
    private String groupId;
}
