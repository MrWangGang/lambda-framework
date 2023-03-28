package org.lamb.framework.sub.openai;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LambOpenAiConversations<T> {

    private long totalPromptTokens = 0L;
    private long totalCompletionTokens = 0L;
    private long totalTokens = 0L;
    private List<LambOpenAiConversation<T>> conversations;

}
