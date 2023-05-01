package org.lambda.framework.sub.openai.service;

import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.sub.openai.LambOpenAiContract;
import org.lamb.framework.sub.openai.LambOpenAiUniqueParam;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;

public interface Function {
    public default LambOpenAiUniqueParam lambOpenAiUniqueId(String userId){
        return LambOpenAiContract.lambOpenAiUniqueId(userId);
    }

    //quata 用户配额
    //prompt 当前会话提示短语
    //maxtoken=prompt+预计返回消耗TOKEN 可以用maxtoken计算 对chat来说=maxtoken,对其他模型来说就是固定算size

    //prompt + response = maxtoken chatgpt 返回的信息不会超过 maxtoken-prompt 所以只要判断配额是否小于maxtoken
    public default boolean limitVerify(Long quota,Integer maxtoken,Integer promptTokens) {
        if(quota<=0)throw new LambEventException(EAI00000100);
        if(maxtoken<=0)throw new LambEventException(EAI00000101);
        if(maxtoken<=promptTokens)throw new LambEventException(EAI00000102);
        if(quota<=maxtoken)throw new LambEventException(EAI00000100);
        return true;
    }

}
