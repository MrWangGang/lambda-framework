package org.lambda.framework.sub.openai;

import lombok.*;

@Getter
@Setter
public class CurrentConversation<T> {
    private long currentPromptTokens = 0L;
    private long currentCompletionTokens = 0L;
    private long currentTotalTokens = 0L;
    private T currentConversation;
}
