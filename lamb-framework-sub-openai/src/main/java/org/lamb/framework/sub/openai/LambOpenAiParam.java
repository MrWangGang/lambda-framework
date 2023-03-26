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
    protected String chatId;
    //用户ID-唯一
    protected String userId;

    public LambOpenAiParam(Long timeOut, String openAiApiKey, String chatId, String userId) {
        this.timeOut = timeOut;
        this.openAiApiKey = openAiApiKey;
        this.chatId = chatId;
        this.userId = userId;
    }
}
