package org.lamb.framework.sub.openai.chat;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.apache.commons.lang3.StringUtils;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.redis.operation.LambReactiveRedisOperation;
import org.lamb.framework.sub.openai.LambOpenAiChatFunction;
import org.lamb.framework.sub.openai.LambOpenAiContract;
import org.lamb.framework.sub.openai.chat.param.LambOpenAiChatParam;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;
import static org.lamb.framework.common.enums.LambExceptionEnum.EAI0000003;
import static org.lamb.framework.sub.openai.LambOpenAiContract.clientTimeOut;
import static org.lamb.framework.sub.openai.LambOpenAiModelEnum.TURBO;

@Component
public class LambOpenAiChatService implements LambOpenAiChatFunction {

    @Resource(name = "lambOpenAiChatRedisTemplate")
    private ReactiveRedisTemplate lambOpenAiChatRedisTemplate;

    @Override
    public Mono<String> execute(LambOpenAiChatParam param) {
        if(param == null)throw new LambEventException(EAI0000003);
        if(StringUtils.isBlank(param.getPrompt()))throw new LambEventException(EAI0000001);
        if(StringUtils.isBlank(param.getChatId()))throw new LambEventException(EAI0000002);
        if(StringUtils.isBlank(param.getOpenAiApiKey()))throw new LambEventException(EAI0000004);
        if(StringUtils.isBlank(param.getUserId()))throw new LambEventException(EAI0000005);
        if(!LambOpenAiContract.verify(param.getUserId(),param.getChatId()))throw new LambEventException(EAI00000008);

        return LambReactiveRedisOperation.build(lambOpenAiChatRedisTemplate).get(param.getChatId())
                .onErrorResume(e->Mono.error(new LambEventException(EAI00000007)))
                .defaultIfEmpty(Mono.empty())
                .flatMap(e->{
                    List<ChatMessage> chatMessages = null;
                    try {
                        if(e.equals(Mono.empty())){
                            //没有历史聊天记录,第一次对话,装载AI人设
                            chatMessages = new LinkedList<>();
                            if(StringUtils.isNotBlank(param.getPersona())){
                                chatMessages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),param.getPersona()));
                            }
                        }else {
                            chatMessages = (List<ChatMessage>) e;
                        }
                        chatMessages.add(new ChatMessage(ChatMessageRole.USER.value(), param.getPrompt()));
                    }catch (Throwable throwable){
                        return Mono.error(new LambEventException(EAI00000006,throwable.getMessage()));
                    }

                    try {
                        Long timeOut = param.getTimeOut();
                        if(param.getTimeOut() == null || param.getTimeOut().longValue() == 0){
                            timeOut = clientTimeOut;
                        }

                        OpenAiService service = new OpenAiService(param.getOpenAiApiKey(),Duration.ofSeconds(timeOut));
                        ChatCompletionRequest request = ChatCompletionRequest.builder()
                                .model(TURBO.getModel())
                                .messages(chatMessages)
                                .temperature(param.getTemperature())
                                .topP(param.getTopP())
                                .n(param.getN())
                                .stream(param.getStream())
                                .maxTokens(param.getMaxTokens())
                                .presencePenalty(param.getPresencePenalty())
                                .frequencyPenalty(param.getFrequencyPenalty())
                                .build();
                        ChatMessage chatMessage = service.createChatCompletion(request).getChoices().get(0).getMessage();
                        chatMessages.add(chatMessage);
                        LambReactiveRedisOperation.build(lambOpenAiChatRedisTemplate).set(param.getChatId(),chatMessages);
                        return Mono.just(chatMessage.getContent());
                    }catch (Throwable throwable){
                        return Mono.error(new LambEventException(EAI00000006,throwable.getMessage()));
                    }
                });
    }
}
