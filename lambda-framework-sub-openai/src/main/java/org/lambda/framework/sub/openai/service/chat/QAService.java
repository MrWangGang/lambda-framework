package org.lambda.framework.sub.openai.service.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.sub.openai.LambOpenAiContract;
import org.lamb.framework.sub.openai.LambOpenAiConversation;
import org.lamb.framework.sub.openai.LambOpenAiConversations;
import org.lamb.framework.sub.openai.LambOpenAiCurrentConversation;
import org.lamb.framework.sub.openai.service.chat.response.LambOpenAiChatMessage;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.lambda.framework.sub.openai.Contract;
import org.lambda.framework.sub.openai.Conversation;
import org.lambda.framework.sub.openai.Conversations;
import org.lambda.framework.sub.openai.CurrentConversation;
import org.lambda.framework.sub.openai.service.chat.param.QAParam;
import org.lambda.framework.sub.openai.service.chat.response.ChatMessageResponse;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;
import static org.lamb.framework.sub.openai.LambOpenAiContract.*;
import static org.lamb.framework.sub.openai.LambOpenAiModelEnum.TURBO;
import static org.lambda.framework.common.enums.ExceptionEnum.EAI00000007;

@Component
public class QAService implements QAFunction {

    @Resource(name = "qARedisTemplate")
    private ReactiveRedisTemplate lambOpenAiQARedisTemplate;
    @Override
    public Mono<CurrentConversation<ChatMessageResponse>> execute(QAParam param) {
        //参数校验
        param.verify();

        String uniqueId = Contract.lambOpenAiUniqueId(param.getUserId(),param.getUniqueParam().getUniqueTime());

        return ReactiveRedisOperation.build(lambOpenAiQARedisTemplate).get(uniqueId)
                .onErrorResume(e->Mono.error(new EventException(EAI00000007)))
                .defaultIfEmpty(Mono.empty())
                .flatMap(e->{
                    List<ChatMessage> chatMessages = null;
                    List<ChatMessageResponse> chatMessageResponse = null;
                    List<Conversation<ChatMessageResponse>> conversation = null;
                    Conversations<ChatMessageResponse> conversations = null;
                    try {
                        Integer tokens = 0;
                        if(e.equals(Mono.empty())){
                            chatMessages = new LinkedList<>();
                            lambOpenAiChatMessages = new LinkedList<>();
                            if(StringUtils.isNotBlank(param.getPersona())){
                                chatMessages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),param.getPersona()));
                                lambOpenAiChatMessages.add(new ChatMessageResponse(ChatMessageRole.SYSTEM.value(),param.getPersona(),currentTime()));
                                tokens = tokens + encoding(param.getPersona());
                            }
                            chatMessages.add(new ChatMessage(ChatMessageRole.USER.value(),param.getPrompt()));
                            lambOpenAiChatMessages.add(new ChatMessageResponse(ChatMessageRole.USER.value(),param.getPrompt(),currentTime()));
                            tokens = tokens + encoding(param.getPrompt());
                            //没有历史聊天记录,第一次对话,装载AI人设
                            lambOpenAiConversation = new LinkedList<>();
                            LambOpenAiConversation<ChatMessageResponse> conversation = new LambOpenAiConversation<LambOpenAiChatMessage>();
                            conversation.setConversation(lambOpenAiChatMessages);
                            Conversation.add(conversation);
                            Conversations = new LambOpenAiConversations<ChatMessageResponse>();
                            Conversations.setConversations(lambOpenAiConversation);
                            if(!limitVerify(param.getQuota(),param.getMaxTokens(),tokens))return Mono.error(new LambEventException(EAI00000100));
                        }else {
                            //QA 每次都是新的对话
                            Conversations = new ObjectMapper().convertValue(e, new TypeReference<>(){});
                            Conversation<ChatMessageResponse> conversation = new LambOpenAiConversation<LambOpenAiChatMessage>();
                            chatMessages = new LinkedList();
                            lambOpenAiChatMessages = new LinkedList<>();
                            if(StringUtils.isNotBlank(param.getPersona())){
                                chatMessages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),param.getPersona()));
                                lambOpenAiChatMessages.add(new LambOpenAiChatMessage(ChatMessageRole.SYSTEM.value(),param.getPersona(),currentTime()));
                                tokens = tokens + encoding(param.getPersona());
                            }
                            chatMessages.add(new ChatMessage(ChatMessageRole.USER.value(),param.getPrompt()));
                            lambOpenAiChatMessages.add(new LambOpenAiChatMessage(ChatMessageRole.USER.value(),param.getPrompt(),currentTime()));
                            tokens = tokens + encoding(param.getPrompt());
                            conversation.setConversation(lambOpenAiChatMessages);
                            lambOpenAiConversations.getConversations().add(conversation);
                            if(!limitVerify(param.getQuota(),param.getMaxTokens(),tokens))return Mono.error(new LambEventException(EAI00000100));
                        }
                    }catch (Throwable throwable){
                        return Mono.error(new LambEventException(EAI00000006,throwable.getMessage()));
                    }

                    try {
                        OpenAiService service = new OpenAiService(param.getOpenAiApiKey(),Duration.ofSeconds(param.getTimeOut()));
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

                        ChatCompletionResult chatCompletionResult = service.createChatCompletion(request);
                        ChatMessage chatMessage = chatCompletionResult.getChoices().get(0).getMessage();
                        LambOpenAiConversation<LambOpenAiChatMessage> conversation = lambOpenAiConversations.getConversations().get(lambOpenAiConversations.getConversations().size()-1);
                        conversation.setPromptTokens(chatCompletionResult.getUsage().getPromptTokens());
                        conversation.setCompletionTokens(chatCompletionResult.getUsage().getCompletionTokens());
                        conversation.setTotalTokens(chatCompletionResult.getUsage().getTotalTokens());
                        conversation.getConversation().add(new LambOpenAiChatMessage(chatMessage.getRole(),chatMessage.getContent(),LambOpenAiContract.currentTime()));

                        lambOpenAiConversations.setTotalTokens(lambOpenAiConversations.getTotalTokens() + chatCompletionResult.getUsage().getTotalTokens());
                        lambOpenAiConversations.setTotalPromptTokens(lambOpenAiConversations.getTotalPromptTokens() + chatCompletionResult.getUsage().getPromptTokens());
                        lambOpenAiConversations.setTotalCompletionTokens(lambOpenAiConversations.getTotalCompletionTokens() + chatCompletionResult.getUsage().getCompletionTokens());

                        org.lamb.framework.redis.operation.ReactiveRedisOperation.build(lambOpenAiQARedisTemplate).set(uniqueId,lambOpenAiConversations);
                        return Mono.just(conversation).flatMap(current->{
                            LambOpenAiCurrentConversation<LambOpenAiChatMessage> currentConversation =  new LambOpenAiCurrentConversation<LambOpenAiChatMessage>();
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
