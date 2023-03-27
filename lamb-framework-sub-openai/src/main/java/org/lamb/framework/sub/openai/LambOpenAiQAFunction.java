package org.lamb.framework.sub.openai;

import org.lamb.framework.sub.openai.chat.param.LambOpenAiChatParam;
import org.lamb.framework.sub.openai.qa.param.LambOpenAiQAParam;
import reactor.core.publisher.Mono;

public interface LambOpenAiQAFunction extends LambOpenAiFunction {
    public Mono<LambOpenAiMessage> execute(LambOpenAiQAParam param);

}
