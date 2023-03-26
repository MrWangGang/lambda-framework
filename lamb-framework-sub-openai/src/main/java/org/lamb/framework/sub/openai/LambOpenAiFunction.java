package org.lamb.framework.sub.openai;

public interface LambOpenAiFunction {
    public default String lambOpenAiUniqueId(String userId){
        return LambOpenAiContract.lambOpenAiUniqueId(userId);
    }
    
}
