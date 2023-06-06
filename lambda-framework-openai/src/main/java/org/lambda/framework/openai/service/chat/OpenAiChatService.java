package org.lambda.framework.openai.service.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.openai.OpenAiContract;
import org.lambda.framework.openai.OpenAiConversation;
import org.lambda.framework.openai.OpenAiConversations;
import org.lambda.framework.openai.OpenAiReplying;
import org.lambda.framework.openai.enums.OpenAiModelEnum;
import org.lambda.framework.openai.enums.OpenaiExceptionEnum;
import org.lambda.framework.openai.service.chat.param.OpenAiChatParam;
import org.lambda.framework.openai.service.chat.response.OpenAiChatReplied;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import static org.lambda.framework.openai.OpenAiContract.currentTime;
import static org.lambda.framework.openai.OpenAiContract.encoding;

@Component
public class OpenAiChatService implements OpenAiChatFunction {

    @Resource(name = "openAiChatRedisOperation")
    private ReactiveRedisOperation openAiChatRedisOperation;

    @Override
    public Mono<OpenAiReplying<OpenAiChatReplied>> execute(OpenAiChatParam param) {
        //参数校验
        param.verify();

        String uniqueId = OpenAiContract.uniqueId(param.getUserId(),param.getUniqueParam().getUniqueTime());

        return openAiChatRedisOperation.get(uniqueId)
                .onErrorResume(e->Mono.error(new EventException(OpenaiExceptionEnum.ES_OPENAI_007)))
                .defaultIfEmpty(Mono.empty())
                .flatMap(e->{
                    List<ChatMessage> chatMessage = null;
                    List<OpenAiChatReplied> openAiChatReplied = null;
                    List<OpenAiConversation<OpenAiChatReplied>> openAiConversation = null;
                    OpenAiConversations<OpenAiChatReplied> openAiConversations = null;

                    Integer tokens = 0;
                    if(e.equals(Mono.empty())){
                        //没有历史聊天记录,第一次对话,装载AI人设
                        chatMessage = new LinkedList<>();
                        openAiChatReplied = new LinkedList<>();
                        if(StringUtils.isNotBlank(param.getPersona())){
                            chatMessage.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),param.getPersona()));
                            openAiChatReplied.add(new OpenAiChatReplied(ChatMessageRole.SYSTEM.value(),param.getPersona(),currentTime()));
                            tokens = tokens + encoding(param.getPersona());
                        }
                        chatMessage.add(new ChatMessage(ChatMessageRole.USER.value(),param.getPrompt()));
                        openAiChatReplied.add(new OpenAiChatReplied(ChatMessageRole.USER.value(),param.getPrompt(),currentTime()));
                        tokens = tokens + encoding(param.getPrompt());
                        openAiConversation = new LinkedList<>();
                        OpenAiConversation<OpenAiChatReplied> _openAiConversation = new OpenAiConversation<OpenAiChatReplied>();
                        _openAiConversation.setConversation(openAiChatReplied);
                        openAiConversation.add(_openAiConversation);
                        openAiConversations = new OpenAiConversations<OpenAiChatReplied>();
                        openAiConversations.setOpenAiConversations(openAiConversation);
                    }else {
                        List<ChatMessage> _chatMessage = new LinkedList<>();
                        openAiConversations = new ObjectMapper().convertValue(e, new TypeReference<>(){});
                        openAiConversations.getOpenAiConversations().forEach(_conversations->{
                            _conversations.getConversation().forEach(message->{
                                _chatMessage.add(new ChatMessage(message.getRole(),message.getContent()));
                            });
                        });
                        chatMessage = _chatMessage;
                        chatMessage.add(new ChatMessage(ChatMessageRole.USER.value(),param.getPrompt()));
                        openAiChatReplied = new LinkedList<>();
                        openAiChatReplied.add(new OpenAiChatReplied(ChatMessageRole.USER.value(),param.getPrompt(),currentTime()));
                        tokens = tokens + encoding(param.getPrompt());
                        OpenAiConversation<OpenAiChatReplied> _openAiConversation = new OpenAiConversation<>();
                        _openAiConversation.setConversation(openAiChatReplied);
                        openAiConversations.getOpenAiConversations().add(_openAiConversation);
                        //多轮对话要计算所有的tokens
                        tokens = Math.toIntExact(tokens + openAiConversations.getTotalTokens());
                    }
                    limitVerify(param.getQuota(),param.getMaxTokens(),tokens);
                    limitVerifyByModel(OpenAiModelEnum.TURBO,param.getQuota(),param.getMaxTokens(),tokens);


                    OpenAiService service = new OpenAiService(param.getApiKey(),Duration.ofSeconds(param.getTimeOut()));
                    ChatCompletionRequest request = ChatCompletionRequest.builder()
                                .model(OpenAiModelEnum.TURBO.getModel())
                                .messages(chatMessage)
                                .temperature(param.getTemperature())
                                .topP(param.getTopP())
                                .n(param.getN())
                                .stream(param.getStream())
                                .maxTokens(param.getMaxTokens())
                                .presencePenalty(param.getPresencePenalty())
                                .frequencyPenalty(param.getFrequencyPenalty())
                                .build();

                    OpenAiConversations<OpenAiChatReplied> finalOpenAiConversations = openAiConversations;
                    return Mono.fromCallable(() -> service.createChatCompletion(request))
                            .onErrorMap(throwable -> new EventException(OpenaiExceptionEnum.ES_OPENAI_006, throwable.getMessage()))
                            .flatMap(chatCompletionResult -> {
                                ChatMessage _chatMessage = chatCompletionResult.getChoices().get(0).getMessage();
                                OpenAiConversation<OpenAiChatReplied> _openAiConversation = finalOpenAiConversations.getOpenAiConversations().get(finalOpenAiConversations.getOpenAiConversations().size()-1);
                                _openAiConversation.setPromptTokens(chatCompletionResult.getUsage().getPromptTokens());
                                _openAiConversation.setCompletionTokens(chatCompletionResult.getUsage().getCompletionTokens());
                                _openAiConversation.setTotalTokens(chatCompletionResult.getUsage().getTotalTokens());
                                _openAiConversation.getConversation().add(new OpenAiChatReplied(_chatMessage.getRole(),_chatMessage.getContent(), OpenAiContract.currentTime()));
                                finalOpenAiConversations.setTotalTokens(finalOpenAiConversations.getTotalTokens() + chatCompletionResult.getUsage().getTotalTokens());
                                finalOpenAiConversations.setTotalPromptTokens(finalOpenAiConversations.getTotalPromptTokens() + chatCompletionResult.getUsage().getPromptTokens());
                                finalOpenAiConversations.setTotalCompletionTokens(finalOpenAiConversations.getTotalCompletionTokens() + chatCompletionResult.getUsage().getCompletionTokens());
                                openAiChatRedisOperation.set(uniqueId, finalOpenAiConversations);
                                return Mono.just(_openAiConversation);
                            })
                            .flatMap(current->{
                                OpenAiReplying<OpenAiChatReplied> openAiReplying =  new OpenAiReplying<OpenAiChatReplied>();
                                openAiReplying.setReplying(current.getConversation().get(current.getConversation().size()-1));
                                openAiReplying.setPromptTokens(current.getPromptTokens());
                                openAiReplying.setCompletionTokens(current.getCompletionTokens());
                                openAiReplying.setTotalTokens(current.getTotalTokens());
                                return Mono.just(openAiReplying);
                            });
                });
    }

}
