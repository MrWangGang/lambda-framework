package org.lamb.framework.sub.openai;

import com.theokanning.openai.image.Image;
import org.lamb.framework.sub.openai.chat.param.LambOpenAiChatParam;
import org.lamb.framework.sub.openai.paint.param.LambOpenAiPaintParam;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LambOpenAiPaintFunction extends LambOpenAiFunction {
    public Mono<LambOpenAiImage> execute(LambOpenAiPaintParam param);

}
