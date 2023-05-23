package org.lambda.framework.openai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenAiReplying<T> {
    private long promptTokens = 0L;
    private long completionTokens = 0L;
    private long totalTokens = 0L;
    private T replying;
}
