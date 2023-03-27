package org.lamb.framework.sub.openai.paint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.service.OpenAiService;
import org.apache.commons.lang3.StringUtils;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.redis.operation.LambReactiveRedisOperation;
import org.lamb.framework.sub.openai.LambOpenAiContract;
import org.lamb.framework.sub.openai.LambOpenAiImage;
import org.lamb.framework.sub.openai.LambOpenAiMessage;
import org.lamb.framework.sub.openai.LambOpenAiPaintFunction;
import org.lamb.framework.sub.openai.paint.param.LambOpenAiPaintParam;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;
import static org.lamb.framework.sub.openai.LambOpenAiContract.clientTimeOut;
import static org.lamb.framework.sub.openai.LambOpenAiContract.currentTime;

@Component
public class LambOpenAiPaintService implements LambOpenAiPaintFunction {

    @Resource(name = "lambOpenAiPaintRedisTemplate")
    private ReactiveRedisTemplate lambOpenAiPaintRedisTemplate;


    @Override
    public Mono<LambOpenAiImage> execute(LambOpenAiPaintParam param) {
        if(param == null)throw new LambEventException(EAI0000003);
        if(StringUtils.isBlank(param.getPrompt()))throw new LambEventException(EAI0000001);
        if(param.getLambOpenAiUniqueParam() == null)throw new LambEventException(EAI0000002);
        if(StringUtils.isBlank(param.getLambOpenAiUniqueParam().getUniqueId()))throw new LambEventException(EAI0000002);
        if(StringUtils.isBlank(param.getLambOpenAiUniqueParam().getUniqueTime()))throw new LambEventException(EAI0000009);
        if(StringUtils.isBlank(param.getOpenAiApiKey()))throw new LambEventException(EAI0000004);
        if(StringUtils.isBlank(param.getUserId()))throw new LambEventException(EAI0000005);
        if(StringUtils.isBlank(param.getResponseFormat()))throw new LambEventException(EAI0000010);


        if(!LambOpenAiContract.verify(param.getUserId(),param.getLambOpenAiUniqueParam()))throw new LambEventException(EAI00000008);

        String uniqueId = LambOpenAiContract.lambOpenAiUniqueId(param.getUserId(),param.getLambOpenAiUniqueParam().getUniqueTime());

        return LambReactiveRedisOperation.build(lambOpenAiPaintRedisTemplate).get(uniqueId)
                .onErrorResume(e->Mono.error(new LambEventException(EAI00000007)))
                .defaultIfEmpty(Mono.empty())
                .flatMap(e->{
                    List<LambOpenAiImage> lambOpenAiImages = null;

                    try {
                        Long timeOut = param.getTimeOut();
                        if(param.getTimeOut() == null || param.getTimeOut().longValue() == 0){
                            timeOut = clientTimeOut;
                        }
                        OpenAiService service = new OpenAiService(param.getOpenAiApiKey(),Duration.ofSeconds(timeOut));
                        CreateImageRequest request = CreateImageRequest.builder()
                                .prompt(param.getPrompt())
                                .size(param.getSize())
                                .n(param.getN())
                                .responseFormat(param.getResponseFormat())
                                .build();
                        List<Image> images = service.createImage(request).getData();
                        if(e.equals(Mono.empty())){
                            //历史记录为空
                            lambOpenAiImages = new LinkedList<>();
                            lambOpenAiImages.add(new LambOpenAiImage(images,param.getPrompt(),currentTime()));
                        }else{
                            lambOpenAiImages = new ObjectMapper().convertValue(e, new TypeReference<List<LambOpenAiImage>>(){});
                            lambOpenAiImages.add(new LambOpenAiImage(images,param.getPrompt(),currentTime()));
                        }

                        LambReactiveRedisOperation.build(lambOpenAiPaintRedisTemplate).set(uniqueId,lambOpenAiImages);
                        return Mono.just(lambOpenAiImages.get(lambOpenAiImages.size()-1));
                    }catch (Throwable throwable){
                        return Mono.error(new LambEventException(EAI00000006,throwable.getMessage()));
                    }
                });
    }
}
