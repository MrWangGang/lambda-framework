package org.lambda.framework.sub.openai;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Conversations<T> {

    private long totalPromptTokens = 0L;
    private long totalCompletionTokens = 0L;
    private long totalTokens = 0L;
    private List<Conversation<T>> conversations;

}
