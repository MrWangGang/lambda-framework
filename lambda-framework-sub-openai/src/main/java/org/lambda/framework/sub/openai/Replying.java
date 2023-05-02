package org.lambda.framework.sub.openai;

import lombok.*;

@Getter
@Setter
public class Replying<T> {
    private long promptTokens = 0L;
    private long completionTokens = 0L;
    private long totalTokens = 0L;
    private T replying;
}
