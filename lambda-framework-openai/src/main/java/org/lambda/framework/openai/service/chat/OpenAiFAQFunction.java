package org.lambda.framework.openai.service.chat;

import org.lambda.framework.openai.OpenAiReplying;
import org.lambda.framework.openai.service.chat.param.OpenAiFAQParam;
import org.lambda.framework.openai.service.OpenAiAbstractFunction;
import org.lambda.framework.openai.service.chat.response.OpenAiChatReplied;
import reactor.core.publisher.Mono;

public interface OpenAiFAQFunction extends OpenAiAbstractFunction {
    public Mono<OpenAiReplying<OpenAiChatReplied>> execute(OpenAiFAQParam param);

}
