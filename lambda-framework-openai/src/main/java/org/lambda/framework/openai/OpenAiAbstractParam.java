package org.lambda.framework.openai;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.openai.enums.OpaiExceptionEnum;

@Getter
@Setter
public class OpenAiAbstractParam {
    public void verify(){
        if(this == null)throw new EventException(OpaiExceptionEnum.ES_OPAI_003);
        if(timeOut== null)throw new EventException(OpaiExceptionEnum.ES_OPAI_015);
        if(StringUtils.isBlank(prompt))throw new EventException(OpaiExceptionEnum.ES_OPAI_000);
        if(uniqueParam == null)throw new EventException(OpaiExceptionEnum.ES_OPAI_003);
        if(StringUtils.isBlank(uniqueParam.getUniqueId()))throw new EventException(OpaiExceptionEnum.ES_OPAI_003);
        if(StringUtils.isBlank(uniqueParam.getUniqueTime()))throw new EventException(OpaiExceptionEnum.ES_OPAI_003);
        if(StringUtils.isBlank(apiKey))throw new EventException(OpaiExceptionEnum.ES_OPAI_004);
        if(StringUtils.isBlank(userId))throw new EventException(OpaiExceptionEnum.ES_OPAI_005);
        if(!OpenAiContract.verify(userId, uniqueParam))throw new EventException(OpaiExceptionEnum.ES_OPAI_008);
        //配额校验
        if(maxTokens == null)throw new EventException(OpaiExceptionEnum.ES_OPAI_012);
        if(n == null)throw new EventException(OpaiExceptionEnum.ES_OPAI_013);
        if(quota == null)throw new EventException(OpaiExceptionEnum.ES_OPAI_014);
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
