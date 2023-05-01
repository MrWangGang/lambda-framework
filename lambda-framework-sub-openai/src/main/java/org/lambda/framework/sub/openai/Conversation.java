package org.lambda.framework.sub.openai;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Conversation<T> {
    private long promptTokens = 0L;
    private long completionTokens = 0L;
    private long totalTokens = 0L;
    private List<T> conversation;
}
