package org.lamb.framework.sub.openai.service.chat;

import org.lamb.framework.sub.openai.LambOpenAiCurrentConversation;
import org.lamb.framework.sub.openai.service.LambOpenAiFunction;
import org.lamb.framework.sub.openai.service.chat.param.LambOpenAiQAParam;
import org.lamb.framework.sub.openai.service.chat.response.LambOpenAiChatMessage;
import reactor.core.publisher.Mono;

public interface LambOpenAiQAFunction extends LambOpenAiFunction {
    public Mono<LambOpenAiCurrentConversation<LambOpenAiChatMessage>> execute(LambOpenAiQAParam param);

}
