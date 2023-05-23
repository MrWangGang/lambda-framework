package org.lambda.framework.openai.service.chat;

import org.lambda.framework.openai.service.chat.param.OpenAiChatParam;
import org.lambda.framework.openai.OpenAiReplying;
import org.lambda.framework.openai.service.OpenAiAbstractFunction;
import org.lambda.framework.openai.service.chat.response.OpenAiChatReplied;
import reactor.core.publisher.Mono;

public interface OpenAiChatFunction extends OpenAiAbstractFunction {
    public Mono<OpenAiReplying<OpenAiChatReplied>> execute(OpenAiChatParam param);

}
