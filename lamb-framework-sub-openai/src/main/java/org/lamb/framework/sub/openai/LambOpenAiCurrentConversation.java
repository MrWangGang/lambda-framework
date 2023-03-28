package org.lamb.framework.sub.openai;

import lombok.*;

import java.util.List;

@Getter
@Setter
public class LambOpenAiCurrentConversation<T> {
    private long currentPromptTokens = 0L;
    private long currentCompletionTokens = 0L;
    private long currentTotalTokens = 0L;
    private T currentConversation;
}
