package org.lambda.framework.sub.openai.service.chat;

import org.lambda.framework.sub.openai.CurrentConversation;
import org.lambda.framework.sub.openai.service.Function;
import org.lambda.framework.sub.openai.service.chat.param.QAParam;
import org.lambda.framework.sub.openai.service.chat.response.ChatMessageResponse;
import reactor.core.publisher.Mono;

public interface QAFunction extends Function {
    public Mono<CurrentConversation<ChatMessageResponse>> execute(QAParam param);

}
