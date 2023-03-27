package org.lamb.framework.sub.openai;

public interface LambOpenAiFunction {
    public default LambOpenAiUniqueParam lambOpenAiUniqueId(String userId){
        return LambOpenAiContract.lambOpenAiUniqueId(userId);
    }
    
}
