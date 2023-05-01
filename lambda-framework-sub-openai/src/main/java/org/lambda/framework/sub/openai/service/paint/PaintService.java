package org.lambda.framework.sub.openai.service.paint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.Resource;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.sub.openai.LambOpenAiContract;
import org.lamb.framework.sub.openai.LambOpenAiConversation;
import org.lamb.framework.sub.openai.LambOpenAiConversations;
import org.lamb.framework.sub.openai.LambOpenAiCurrentConversation;
import org.lamb.framework.sub.openai.service.paint.param.LambOpenAiPaintParam;
import org.lamb.framework.sub.openai.service.paint.response.LambOpenAiImage;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;
import static org.lamb.framework.sub.openai.LambOpenAiContract.*;

@Component
public class PaintService implements PaintFunction {

    @Resource(name = "lambOpenAiPaintRedisTemplate")
    private ReactiveRedisTemplate lambOpenAiPaintRedisTemplate;
    @Override
    public Mono<LambOpenAiCurrentConversation<LambOpenAiImage>> execute(LambOpenAiPaintParam param) {
        //参数校验
        param.verify();
        //对于图片模型的maxToken的计算返回的图片数量*设置size + prompt
        Integer promptTokens = encoding(param.getPrompt());
        if(!limitVerify(param.getQuota(),param.getMaxTokens(),encoding(param.getPrompt())))throw new LambEventException(EAI00000100);

        if(!LambOpenAiContract.verify(param.getUserId(),param.getLambOpenAiUniqueParam()))throw new LambEventException(EAI00000008);

        String uniqueId = LambOpenAiContract.lambOpenAiUniqueId(param.getUserId(),param.getLambOpenAiUniqueParam().getUniqueTime());

        return org.lamb.framework.redis.operation.ReactiveRedisOperation.build(lambOpenAiPaintRedisTemplate).get(uniqueId)
                .onErrorResume(e->Mono.error(new LambEventException(EAI00000007)))
                .defaultIfEmpty(Mono.empty())
                .flatMap(e->{
                    List<LambOpenAiImage> lambOpenAiImages = null;
                    List<LambOpenAiConversation<LambOpenAiImage>> lambOpenAiConversation = null;
                    LambOpenAiConversations<LambOpenAiImage> lambOpenAiConversations = null;
                    try {
                        if(e.equals(Mono.empty())){
                            //历史记录为空
                            lambOpenAiImages = new LinkedList<>();
                            lambOpenAiImages.add(new LambOpenAiImage(null,param.getPrompt(),currentTime()));

                            lambOpenAiConversation = new LinkedList<>();
                            LambOpenAiConversation<LambOpenAiImage> conversation = new LambOpenAiConversation<LambOpenAiImage>();
                            conversation.setConversation(lambOpenAiImages);
                            lambOpenAiConversation.add(conversation);

                            lambOpenAiConversations = new LambOpenAiConversations<LambOpenAiImage>();
                            lambOpenAiConversations.setConversations(lambOpenAiConversation);
                        }else{
                            lambOpenAiImages = new LinkedList<>();
                            lambOpenAiImages.add(new LambOpenAiImage(null,param.getPrompt(),currentTime()));
                            lambOpenAiConversations = new ObjectMapper().convertValue(e, new TypeReference<>(){});
                            lambOpenAiConversation = lambOpenAiConversations.getConversations();
                            LambOpenAiConversation<LambOpenAiImage> conversation = new LambOpenAiConversation<LambOpenAiImage>();
                            conversation.setConversation(lambOpenAiImages);
                            lambOpenAiConversation.add(conversation);
                        }

                        OpenAiService service = new OpenAiService(param.getOpenAiApiKey(),Duration.ofSeconds(param.getTimeOut()));
                        CreateImageRequest request = CreateImageRequest.builder()
                                .prompt(param.getPrompt())
                                .size(param.getSize())
                                .n(param.getN())
                                .responseFormat(param.getResponseFormat())
                                .build();
                        ImageResult imageResult  = service.createImage(request);
                        lambOpenAiImages.get(lambOpenAiImages.size()-1).setImages(imageResult.getData());
                        LambOpenAiConversation<LambOpenAiImage> conversation = lambOpenAiConversations.getConversations().get(lambOpenAiConversations.getConversations().size()-1);

                        Integer completionTokens = imageTokens(param.getSize(),param.getN());
                        Integer totalTokens =promptTokens + completionTokens;
                        conversation.setPromptTokens(promptTokens);
                        conversation.setCompletionTokens(completionTokens);
                        conversation.setTotalTokens(totalTokens);
                        lambOpenAiConversations.setTotalTokens(lambOpenAiConversations.getTotalTokens() + totalTokens);
                        lambOpenAiConversations.setTotalPromptTokens(lambOpenAiConversations.getTotalPromptTokens() + promptTokens);
                        lambOpenAiConversations.setTotalCompletionTokens(lambOpenAiConversations.getTotalCompletionTokens() + completionTokens);
                        org.lamb.framework.redis.operation.ReactiveRedisOperation.build(lambOpenAiPaintRedisTemplate).set(uniqueId,lambOpenAiConversations);
                        return Mono.just(conversation).flatMap(current->{
                            LambOpenAiCurrentConversation<LambOpenAiImage> currentConversation =  new LambOpenAiCurrentConversation<LambOpenAiImage>();
                            currentConversation.setCurrentConversation(current.getConversation().get(current.getConversation().size()-1));
                            currentConversation.setCurrentPromptTokens(current.getPromptTokens());
                            currentConversation.setCurrentCompletionTokens(current.getCompletionTokens());
                            currentConversation.setCurrentTotalTokens(current.getTotalTokens());
                            return Mono.just(currentConversation);
                        });


                    }catch (Throwable throwable){
                        return Mono.error(new LambEventException(EAI00000006,throwable.getMessage()));
                    }
                });
    }
}
