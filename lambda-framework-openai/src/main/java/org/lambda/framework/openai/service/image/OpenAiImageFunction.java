package org.lambda.framework.openai.service.image;

import org.lambda.framework.openai.OpenAiReplying;
import org.lambda.framework.openai.service.image.param.OpenAiImageParam;
import org.lambda.framework.openai.service.image.response.OpenAiImageReplied;
import org.lambda.framework.openai.service.OpenAiAbstractFunction;
import reactor.core.publisher.Mono;

public interface OpenAiImageFunction extends OpenAiAbstractFunction {
    public  Mono<OpenAiReplying<OpenAiImageReplied>> execute(OpenAiImageParam param);

}
