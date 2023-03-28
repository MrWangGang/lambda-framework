package org.lamb.framework.sub.openai;

import lombok.*;
import lombok.experimental.Tolerate;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

@Getter
@Setter
public class LambOpenAiParam {


    //配额
    private Long quota;
    //要在聊天完成中生成的最大令牌数。 输入令牌和生成的令牌的总长度受模型上下文长度的限制。

    private Integer maxTokens;
    //Seconds

    protected Long timeOut;

    //openAiKey
    protected String openAiApiKey;
    //保持会话的核心ID
    //用户ID-唯一
    protected String userId;

    protected LambOpenAiUniqueParam lambOpenAiUniqueParam;


    public LambOpenAiParam(Long quota, Integer maxTokens, Long timeOut, String openAiApiKey, String userId, LambOpenAiUniqueParam lambOpenAiUniqueParam) {
        this.quota = quota;
        this.maxTokens = maxTokens;
        this.timeOut = timeOut;
        this.openAiApiKey = openAiApiKey;
        this.userId = userId;
        this.lambOpenAiUniqueParam = lambOpenAiUniqueParam;
    }
}
