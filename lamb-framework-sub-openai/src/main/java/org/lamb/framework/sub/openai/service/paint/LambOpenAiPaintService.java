package org.lamb.framework.sub.openai.service.paint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.ModelType;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import org.apache.commons.lang3.StringUtils;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.redis.operation.LambReactiveRedisOperation;
import org.lamb.framework.sub.openai.LambOpenAiContract;
import org.lamb.framework.sub.openai.LambOpenAiConversation;
import org.lamb.framework.sub.openai.LambOpenAiConversations;
import org.lamb.framework.sub.openai.LambOpenAiCurrentConversation;
import org.lamb.framework.sub.openai.service.chat.response.LambOpenAiChatMessage;
import org.lamb.framework.sub.openai.service.paint.param.LambOpenAiPaintParam;
import org.lamb.framework.sub.openai.service.paint.response.LambOpenAiImage;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;
import static org.lamb.framework.sub.openai.LambOpenAiContract.*;

@Component
public class LambOpenAiPaintService implements LambOpenAiPaintFunction {

    @Resource(name = "lambOpenAiPaintRedisTemplate")
    private ReactiveRedisTemplate lambOpenAiPaintRedisTemplate;
    @Override
    public Mono<LambOpenAiCurrentConversation<LambOpenAiImage>> execute(LambOpenAiPaintParam param) {
        if(param == null)throw new LambEventException(EAI0000003);
        if(param.getTimeOut() == null)throw new LambEventException(EAI0000016);
        if(StringUtils.isBlank(param.getPrompt()))throw new LambEventException(EAI0000001);
        if(param.getLambOpenAiUniqueParam() == null)throw new LambEventException(EAI0000002);
        if(StringUtils.isBlank(param.getLambOpenAiUniqueParam().getUniqueId()))throw new LambEventException(EAI0000002);
        if(StringUtils.isBlank(param.getLambOpenAiUniqueParam().getUniqueTime()))throw new LambEventException(EAI0000009);
        if(StringUtils.isBlank(param.getOpenAiApiKey()))throw new LambEventException(EAI0000004);
        if(StringUtils.isBlank(param.getUserId()))throw new LambEventException(EAI0000005);
        if(StringUtils.isBlank(param.getResponseFormat()))throw new LambEventException(EAI0000010);
        if(StringUtils.isBlank(param.getSize()))throw new LambEventException(EAI0000012);
        //对于图片模型的maxToken的计算返回的图片数量*设置size + prompt
        Integer promptTokens = encoding(param.getPrompt());
        param.setMaxTokens(imageTokens(param.getSize(),param.getN()) + promptTokens);
        if(param.getMaxTokens() == null)throw new LambEventException(EAI0000013);
        if(param.getQuota() == null)throw new LambEventException(EAI00000015);
        if(!limitVerify(param.getQuota(),param.getMaxTokens(),promptTokens))throw new LambEventException(EAI00000100);


        switch (param.getSize()){
            case image_size_256:break;
            case image_size_512:break;
            case image_size_1024:break;
            default:throw new LambEventException(EAI0000011);
        }

        if(!LambOpenAiContract.verify(param.getUserId(),param.getLambOpenAiUniqueParam()))throw new LambEventException(EAI00000008);

        String uniqueId = LambOpenAiContract.lambOpenAiUniqueId(param.getUserId(),param.getLambOpenAiUniqueParam().getUniqueTime());

        return LambReactiveRedisOperation.build(lambOpenAiPaintRedisTemplate).get(uniqueId)
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
                        LambReactiveRedisOperation.build(lambOpenAiPaintRedisTemplate).set(uniqueId,lambOpenAiConversations);
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
