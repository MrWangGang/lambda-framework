package org.lambda.framework.sub.openai.service.image;

import org.lambda.framework.sub.openai.Replying;
import org.lambda.framework.sub.openai.service.AbstractFunction;
import org.lambda.framework.sub.openai.service.image.param.ImageParam;
import org.lambda.framework.sub.openai.service.image.response.ImageReplied;
import reactor.core.publisher.Mono;

public interface ImageFunction extends AbstractFunction {
    public  Mono<Replying<ImageReplied>> execute(ImageParam param);

}
