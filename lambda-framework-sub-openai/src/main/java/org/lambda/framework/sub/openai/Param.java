package org.lambda.framework.sub.openai;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.lamb.framework.common.exception.LambEventException;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;
import static org.lamb.framework.common.enums.LambExceptionEnum.EAI00000015;

@Getter
@Setter
public class Param {
    public void verify(){
        if(this == null)throw new LambEventException(EAI0000003);
        if(timeOut== null)throw new LambEventException(EAI0000016);
        if(StringUtils.isBlank(prompt))throw new LambEventException(EAI0000001);
        if(uniqueParam == null)throw new LambEventException(EAI0000002);
        if(StringUtils.isBlank(uniqueParam.getUniqueId()))throw new LambEventException(EAI0000002);
        if(StringUtils.isBlank(uniqueParam.getUniqueTime()))throw new LambEventException(EAI0000009);
        if(StringUtils.isBlank(openAiApiKey))throw new LambEventException(EAI0000004);
        if(StringUtils.isBlank(userId))throw new LambEventException(EAI0000005);
        if(!Contract.verify(userId, uniqueParam))throw new LambEventException(EAI00000008);
        //配额校验
        if(maxTokens == null)throw new LambEventException(EAI0000013);
        if(n == null)throw new LambEventException(EAI00000014);
        if(quota == null)throw new LambEventException(EAI00000015);
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
    protected String openAiApiKey;
    //保持会话的核心ID
    //用户ID-唯一
    protected String userId;

    protected UniqueParam uniqueParam;

    public Param(Integer n, String prompt, Long quota, Integer maxTokens, Long timeOut, String openAiApiKey, String userId, UniqueParam uniqueParam) {
        this.n = n;
        this.prompt = prompt;
        this.quota = quota;
        this.maxTokens = maxTokens;
        this.timeOut = timeOut;
        this.openAiApiKey = openAiApiKey;
        this.userId = userId;
        this.uniqueParam = uniqueParam;
    }
}
