package org.lamb.framework.sub.openai;

import lombok.*;

@Getter
@Setter
public class LambOpenAiParam {
    //Seconds
    protected Long timeOut;

    //openAiKey
    protected String openAiApiKey;
    //保持会话的核心ID
    //用户ID-唯一
    protected String userId;

    protected LambOpenAiUniqueParam lambOpenAiUniqueParam;

    public LambOpenAiParam(Long timeOut, String openAiApiKey, String userId, LambOpenAiUniqueParam lambOpenAiUniqueParam) {
        this.timeOut = timeOut;
        this.openAiApiKey = openAiApiKey;
        this.userId = userId;
        this.lambOpenAiUniqueParam = lambOpenAiUniqueParam;
    }
}
