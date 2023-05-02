package org.lambda.framework.sub.openai.service.image;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.lambda.framework.sub.openai.Conversation;
import org.lambda.framework.sub.openai.Conversations;
import org.lambda.framework.sub.openai.Replying;
import org.lambda.framework.sub.openai.service.image.param.ImageParam;
import org.lambda.framework.sub.openai.service.image.response.ImageReplied;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import static org.lambda.framework.common.enums.ExceptionEnum.*;
import static org.lambda.framework.sub.openai.Contract.*;

@Component
public class ImageService implements ImageFunction {

    @Resource(name = "openAiImageRedisTemplate")
    private ReactiveRedisTemplate openAiImageRedisTemplate;
    @Override
    public Mono<Replying<ImageReplied>> execute(ImageParam param) {
        //参数校验
        param.verify();
        //对于图片模型的maxToken的计算返回的图片数量*设置size + prompt
        Integer promptTokens = encoding(param.getPrompt());
        if(!limitVerify(param.getQuota(),param.getMaxTokens(),encoding(param.getPrompt())))throw new EventException(EAI00000100);

        if(!verify(param.getUserId(),param.getUniqueParam()))throw new EventException(EAI00000008);

        String uniqueId = uniqueId(param.getUserId(),param.getUniqueParam().getUniqueTime());

        return ReactiveRedisOperation.build(openAiImageRedisTemplate).get(uniqueId)
                .onErrorResume(e->Mono.error(new EventException(EAI00000007)))
                .defaultIfEmpty(Mono.empty())
                .flatMap(e->{
                    List<ImageReplied> imageReplied = null;
                    List<Conversation<ImageReplied>> conversation = null;
                    Conversations<ImageReplied> conversations = null;
                    try {
                        if(e.equals(Mono.empty())){
                            //历史记录为空
                            imageReplied = new LinkedList<>();
                            imageReplied.add(new ImageReplied(null,param.getPrompt(),currentTime()));

                            conversation = new LinkedList<>();
                            Conversation<ImageReplied> _conversation = new Conversation<ImageReplied>();
                            _conversation.setConversation(imageReplied);
                            conversation.add(_conversation);

                            conversations = new Conversations<ImageReplied>();
                            conversations.setConversations(conversation);
                        }else{
                            imageReplied = new LinkedList<>();
                            imageReplied.add(new ImageReplied(null,param.getPrompt(),currentTime()));
                            conversations = new ObjectMapper().convertValue(e, new TypeReference<>(){});
                            conversation = conversations.getConversations();
                            Conversation<ImageReplied> _conversation = new Conversation<ImageReplied>();
                            _conversation.setConversation(imageReplied);
                            conversation.add(_conversation);
                        }

                        OpenAiService service = new OpenAiService(param.getOpenAiApiKey(),Duration.ofSeconds(param.getTimeOut()));
                        CreateImageRequest request = CreateImageRequest.builder()
                                .prompt(param.getPrompt())
                                .size(param.getSize())
                                .n(param.getN())
                                .responseFormat(param.getResponseFormat())
                                .build();
                        ImageResult imageResult  = service.createImage(request);
                        imageReplied.get(imageReplied.size()-1).setImages(imageResult.getData());
                        Conversation<ImageReplied> _conversation = conversations.getConversations().get(conversations.getConversations().size()-1);

                        Integer completionTokens = imageTokens(param.getSize(),param.getN());
                        Integer totalTokens =promptTokens + completionTokens;
                        _conversation.setPromptTokens(promptTokens);
                        _conversation.setCompletionTokens(completionTokens);
                        _conversation.setTotalTokens(totalTokens);
                        conversations.setTotalTokens(conversations.getTotalTokens() + totalTokens);
                        conversations.setTotalPromptTokens(conversations.getTotalPromptTokens() + promptTokens);
                        conversations.setTotalCompletionTokens(conversations.getTotalCompletionTokens() + completionTokens);
                        ReactiveRedisOperation.build(openAiImageRedisTemplate).set(uniqueId,conversations);
                        return Mono.just(_conversation).flatMap(current->{
                            Replying<ImageReplied> replying =  new Replying<ImageReplied>();
                            replying.setReplying(current.getConversation().get(current.getConversation().size()-1));
                            replying.setPromptTokens(current.getPromptTokens());
                            replying.setCompletionTokens(current.getCompletionTokens());
                            replying.setTotalTokens(current.getTotalTokens());
                            return Mono.just(replying);
                        });


                    }catch (Throwable throwable){
                        return Mono.error(new EventException(EAI00000006,throwable.getMessage()));
                    }
                });
    }
}
