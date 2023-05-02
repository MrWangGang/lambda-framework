package org.lambda.framework.sub.openai.service.chat;

import org.lambda.framework.sub.openai.OpenAiReplying;
import org.lambda.framework.sub.openai.service.OpenAiAbstractFunction;
import org.lambda.framework.sub.openai.service.chat.param.OpenAiFAQParam;
import org.lambda.framework.sub.openai.service.chat.response.OpenAiChatReplied;
import reactor.core.publisher.Mono;

public interface OpenAiFAQFunction extends OpenAiAbstractFunction {
    public Mono<OpenAiReplying<OpenAiChatReplied>> execute(OpenAiFAQParam param);

}
