package org.lambda.framework.sub.openai.service.chat;

import org.lambda.framework.sub.openai.Replying;
import org.lambda.framework.sub.openai.service.AbstractFunction;
import org.lambda.framework.sub.openai.service.chat.param.FAQParam;
import org.lambda.framework.sub.openai.service.chat.response.ChatReplied;
import reactor.core.publisher.Mono;

public interface FAQServiceFunction extends AbstractFunction {
    public Mono<Replying<ChatReplied>> execute(FAQParam param);

}
