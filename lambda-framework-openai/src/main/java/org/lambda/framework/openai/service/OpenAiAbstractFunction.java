package org.lambda.framework.openai.service;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.openai.OpenAiContract;
import org.lambda.framework.openai.OpenAiUniqueParam;
import org.lambda.framework.openai.enums.OpenAiModelEnum;
import org.lambda.framework.openai.enums.OpenaiExceptionEnum;

public interface OpenAiAbstractFunction {

    public default OpenAiUniqueParam uniqueId(String userId){
        return OpenAiContract.uniqueId(userId);
    }
    //quata 用户配额
    //prompt 当前会话提示短语
    //maxtoken=prompt+预计返回消耗TOKEN 可以用maxtoken计算 对chat来说=maxtoken,对其他模型来说就是固定算size
    //prompt + response = maxtoken chatgpt 返回的信息不会超过 maxtoken-prompt 所以只要判断配额是否小于maxtoken
    public default boolean limitVerify(Long quota,Integer maxtoken,Integer promptTokens) {
        if(quota<=0)throw new EventException(OpenaiExceptionEnum.ES_OPENAI_016);
        if(maxtoken<=0)throw new EventException(OpenaiExceptionEnum.ES_OPENAI_017);
        if(quota<=maxtoken)throw new EventException(OpenaiExceptionEnum.ES_OPENAI_016);
        return true;
    }

    public default boolean limitVerifyByModel(OpenAiModelEnum model, Long quota, Integer maxtoken, Integer promptTokens) {
        if(model.getModel().equals(OpenAiModelEnum.TURBO.getModel())){
            // This model's maximum context length is 4097 tokens. However, you requested 5180 tokens (1084 in the messages, 4096 in the completion)
            //从这个错误信息看出，maxtoken参与了  prompt+maxtoken <=4096 的校验
            if(promptTokens>=model.getMaxToken()-maxtoken)throw new EventException(OpenaiExceptionEnum.ES_OPENAI_018);
            return true;
        }
        throw new EventException(OpenaiExceptionEnum.ES_OPENAI_019);
    }
}
