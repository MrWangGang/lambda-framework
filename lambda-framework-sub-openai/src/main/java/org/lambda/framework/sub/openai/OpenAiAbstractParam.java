package org.lambda.framework.sub.openai;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.common.enums.ExceptionEnum.*;

@Getter
@Setter
public class OpenAiAbstractParam {
    public void verify(){
        if(this == null)throw new EventException(EAI0000003);
        if(timeOut== null)throw new EventException(EAI0000016);
        if(StringUtils.isBlank(prompt))throw new EventException(EAI0000001);
        if(uniqueParam == null)throw new EventException(EAI0000002);
        if(StringUtils.isBlank(uniqueParam.getUniqueId()))throw new EventException(EAI0000002);
        if(StringUtils.isBlank(uniqueParam.getUniqueTime()))throw new EventException(EAI0000009);
        if(StringUtils.isBlank(apiKey))throw new EventException(EAI0000004);
        if(StringUtils.isBlank(userId))throw new EventException(EAI0000005);
        if(!OpenAiContract.verify(userId, uniqueParam))throw new EventException(EAI00000008);
        //配额校验
        if(maxTokens == null)throw new EventException(EAI0000013);
        if(n == null)throw new EventException(EAI00000014);
        if(quota == null)throw new EventException(EAI00000015);
    }
    //要为每个输入消息生成的聊天完成选项数。
    private Integer n;
    //用户提示
    private String prompt;
    //配额
    private Long quota;
    //要在聊天完成中生成的最大令牌数。 输入令牌和生成的令牌的总长度受模型上下文长度的限制。

    private Integer maxTokens;
    //Seconds

    protected Long timeOut;

    //openAiKey
    protected String apiKey;
    //保持会话的核心ID
    //用户ID-唯一
    protected String userId;

    protected OpenAiUniqueParam uniqueParam;

    public OpenAiAbstractParam(Integer n, String prompt, Long quota, Integer maxTokens, Long timeOut, String apiKey, String userId, OpenAiUniqueParam uniqueParam) {
        this.n = n;
        this.prompt = prompt;
        this.quota = quota;
        this.maxTokens = maxTokens;
        this.timeOut = timeOut;
        this.apiKey = apiKey;
        this.userId = userId;
        this.uniqueParam = uniqueParam;
    }
}
