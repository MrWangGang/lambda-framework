package org.lambda.framework.sub.openai.service.chat;

import com.theokanning.openai.completion.chat.ChatMessage;
import org.lambda.framework.sub.openai.CurrentConversation;
import org.lambda.framework.sub.openai.service.Function;
import org.lambda.framework.sub.openai.service.chat.param.ChatParam;
import org.lambda.framework.sub.openai.service.chat.response.ChatMessageResponse;
import reactor.core.publisher.Mono;

public interface ChatFunction extends Function {
    public Mono<CurrentConversation<ChatMessageResponse>> execute(ChatParam param);

}
