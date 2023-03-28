package org.lamb.framework.sub.openai.service.paint;

import org.lamb.framework.sub.openai.LambOpenAiConversation;
import org.lamb.framework.sub.openai.LambOpenAiConversations;
import org.lamb.framework.sub.openai.LambOpenAiCurrentConversation;
import org.lamb.framework.sub.openai.service.LambOpenAiFunction;
import org.lamb.framework.sub.openai.service.chat.response.LambOpenAiChatMessage;
import org.lamb.framework.sub.openai.service.paint.response.LambOpenAiImage;
import org.lamb.framework.sub.openai.service.paint.param.LambOpenAiPaintParam;
import reactor.core.publisher.Mono;

public interface LambOpenAiPaintFunction extends LambOpenAiFunction {
    public  Mono<LambOpenAiCurrentConversation<LambOpenAiImage>> execute(LambOpenAiPaintParam param);

}
