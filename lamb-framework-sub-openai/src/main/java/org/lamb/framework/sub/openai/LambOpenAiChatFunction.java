package org.lamb.framework.sub.openai;

import org.lamb.framework.sub.openai.chat.param.LambOpenAiChatParam;
import reactor.core.publisher.Mono;

public interface LambOpenAiChatFunction extends LambOpenAiFunction {
    public Mono<String> execute(LambOpenAiChatParam param);

}
