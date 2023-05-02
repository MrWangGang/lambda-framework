package org.lambda.framework.sub.openai.service.image;

import org.lambda.framework.sub.openai.OpenAiReplying;
import org.lambda.framework.sub.openai.service.OpenAiAbstractFunction;
import org.lambda.framework.sub.openai.service.image.param.OpenAiImageParam;
import org.lambda.framework.sub.openai.service.image.response.OpenAiImageReplied;
import reactor.core.publisher.Mono;

public interface OpenAiImageFunction extends OpenAiAbstractFunction {
    public  Mono<OpenAiReplying<OpenAiImageReplied>> execute(OpenAiImageParam param);

}
