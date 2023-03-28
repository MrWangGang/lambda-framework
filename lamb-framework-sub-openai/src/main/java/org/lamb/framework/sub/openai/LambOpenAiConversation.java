package org.lamb.framework.sub.openai;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.lamb.framework.sub.openai.service.chat.response.LambOpenAiChatMessage;

import java.util.List;

@Getter
@Setter
public class LambOpenAiConversation<T> {
    private long promptTokens = 0L;
    private long completionTokens = 0L;
    private long totalTokens = 0L;
    private List<T> conversation;
}
