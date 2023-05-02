package org.lambda.framework.sub.openai.service;

import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.common.enums.ExceptionEnum.*;

public interface AbstractFunction {


    //quata 用户配额
    //prompt 当前会话提示短语
    //maxtoken=prompt+预计返回消耗TOKEN 可以用maxtoken计算 对chat来说=maxtoken,对其他模型来说就是固定算size

    //prompt + response = maxtoken chatgpt 返回的信息不会超过 maxtoken-prompt 所以只要判断配额是否小于maxtoken
    public default boolean limitVerify(Long quota,Integer maxtoken,Integer promptTokens) {
        if(quota<=0)throw new EventException(EAI00000100);
        if(maxtoken<=0)throw new EventException(EAI00000101);
        if(maxtoken<=promptTokens)throw new EventException(EAI00000102);
        if(quota<=maxtoken)throw new EventException(EAI00000100);
        return true;
    }

}
