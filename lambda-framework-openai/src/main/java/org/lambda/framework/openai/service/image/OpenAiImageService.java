package org.lambda.framework.openai.service.image;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.openai.OpenAiContract;
import org.lambda.framework.openai.OpenAiConversation;
import org.lambda.framework.openai.OpenAiConversations;
import org.lambda.framework.openai.OpenAiReplying;
import org.lambda.framework.openai.enums.OpaiExceptionEnum;
import org.lambda.framework.openai.service.image.param.OpenAiImageParam;
import org.lambda.framework.openai.service.image.response.OpenAiImageReplied;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

@Component
public class OpenAiImageService implements OpenAiImageFunction {

    @Resource(name = "openAiImageRedisTemplate")
    private ReactiveRedisTemplate openAiImageRedisTemplate;
    @Override
    public Mono<OpenAiReplying<OpenAiImageReplied>> execute(OpenAiImageParam param) {
        //参数校验
        param.verify();
        //对于图片模型的maxToken的计算返回的图片数量*设置size + prompt
        Integer promptTokens = OpenAiContract.encoding(param.getPrompt());
        if(!limitVerify(param.getQuota(),param.getMaxTokens(), OpenAiContract.encoding(param.getPrompt())))throw new EventException(OpaiExceptionEnum.ES_OPAI_016);

        if(!OpenAiContract.verify(param.getUserId(),param.getUniqueParam()))throw new EventException(OpaiExceptionEnum.ES_OPAI_008);

        String uniqueId = OpenAiContract.uniqueId(param.getUserId(),param.getUniqueParam().getUniqueTime());

        return ReactiveRedisOperation.build(openAiImageRedisTemplate).get(uniqueId)
                .onErrorResume(e->Mono.error(new EventException(OpaiExceptionEnum.ES_OPAI_007)))
                .defaultIfEmpty(Mono.empty())
                .flatMap(e->{
                    List<OpenAiImageReplied> openAiImageReplied = null;
                    List<OpenAiConversation<OpenAiImageReplied>> openAiConversation = null;
                    OpenAiConversations<OpenAiImageReplied> openAiConversations = null;
                        if(e.equals(Mono.empty())){
                            //历史记录为空
                            openAiImageReplied = new LinkedList<>();
                            openAiImageReplied.add(new OpenAiImageReplied(null,param.getPrompt(), OpenAiContract.currentTime()));

                            openAiConversation = new LinkedList<>();
                            OpenAiConversation<OpenAiImageReplied> _openAiConversation = new OpenAiConversation<OpenAiImageReplied>();
                            _openAiConversation.setConversation(openAiImageReplied);
                            openAiConversation.add(_openAiConversation);

                            openAiConversations = new OpenAiConversations<OpenAiImageReplied>();
                            openAiConversations.setOpenAiConversations(openAiConversation);
                        }else{
                            openAiImageReplied = new LinkedList<>();
                            openAiImageReplied.add(new OpenAiImageReplied(null,param.getPrompt(), OpenAiContract.currentTime()));
                            openAiConversations = new ObjectMapper().convertValue(e, new TypeReference<>(){});
                            openAiConversation = openAiConversations.getOpenAiConversations();
                            OpenAiConversation<OpenAiImageReplied> _openAiConversation = new OpenAiConversation<OpenAiImageReplied>();
                            _openAiConversation.setConversation(openAiImageReplied);
                            openAiConversation.add(_openAiConversation);
                        }


                        OpenAiService service = new OpenAiService(param.getApiKey(),Duration.ofSeconds(param.getTimeOut()));
                        CreateImageRequest request = CreateImageRequest.builder()
                                .prompt(param.getPrompt())
                                .size(param.getSize())
                                .n(param.getN())
                                .responseFormat(param.getResponseFormat())
                                .build();
                    OpenAiConversations<OpenAiImageReplied> finalOpenAiConversations = openAiConversations;
                    List<OpenAiImageReplied> finalOpenAiImageReplied = openAiImageReplied;
                    return Mono.fromCallable(() -> service.createImage(request))
                            .onErrorMap(throwable -> new EventException(OpaiExceptionEnum.ES_OPAI_006, throwable.getMessage()))
                            .flatMap(imageResult -> {
                                finalOpenAiImageReplied.get(finalOpenAiImageReplied.size()-1).setImages(imageResult.getData());
                                OpenAiConversation<OpenAiImageReplied> _openAiConversation = finalOpenAiConversations.getOpenAiConversations().get(finalOpenAiConversations.getOpenAiConversations().size()-1);
                                Integer completionTokens = OpenAiContract.imageTokens(param.getSize(),param.getN());
                                Integer totalTokens =promptTokens + completionTokens;
                                _openAiConversation.setPromptTokens(promptTokens);
                                _openAiConversation.setCompletionTokens(completionTokens);
                                _openAiConversation.setTotalTokens(totalTokens);
                                finalOpenAiConversations.setTotalTokens(finalOpenAiConversations.getTotalTokens() + totalTokens);
                                finalOpenAiConversations.setTotalPromptTokens(finalOpenAiConversations.getTotalPromptTokens() + promptTokens);
                                finalOpenAiConversations.setTotalCompletionTokens(finalOpenAiConversations.getTotalCompletionTokens() + completionTokens);
                                ReactiveRedisOperation.build(openAiImageRedisTemplate).set(uniqueId, finalOpenAiConversations);
                                return Mono.just(_openAiConversation);
                            }).flatMap(current->{
                                OpenAiReplying<OpenAiImageReplied> openAiReplying =  new OpenAiReplying<OpenAiImageReplied>();
                                openAiReplying.setReplying(current.getConversation().get(current.getConversation().size()-1));
                                openAiReplying.setPromptTokens(current.getPromptTokens());
                                openAiReplying.setCompletionTokens(current.getCompletionTokens());
                                openAiReplying.setTotalTokens(current.getTotalTokens());
                                return Mono.just(openAiReplying);
                            });
                });
    }
}
