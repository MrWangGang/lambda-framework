package org.lamb.framework.sub.openai.service.chat;

import org.lamb.framework.sub.openai.LambOpenAiCurrentConversation;
import org.lamb.framework.sub.openai.service.LambOpenAiFunction;
import org.lamb.framework.sub.openai.service.chat.param.LambOpenAiChatParam;
import org.lamb.framework.sub.openai.service.chat.response.LambOpenAiChatMessage;
import reactor.core.publisher.Mono;

public interface LambOpenAiChatFunction extends LambOpenAiFunction {
    public Mono<LambOpenAiCurrentConversation<LambOpenAiChatMessage>> execute(LambOpenAiChatParam param);

}
