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
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.lambda.framework.sub.openai.Contract;
import org.lambda.framework.sub.openai.Conversation;
import org.lambda.framework.sub.openai.Conversations;
import org.lambda.framework.sub.openai.CurrentConversation;
import org.lambda.framework.sub.openai.service.chat.param.ChatParam;
import org.lambda.framework.sub.openai.service.chat.response.ChatMessageResponse;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import static org.lambda.framework.common.enums.ExceptionEnum.*;
import static org.lambda.framework.sub.openai.Contract.currentTime;
import static org.lambda.framework.sub.openai.Contract.encoding;
import static org.lambda.framework.sub.openai.ModelEnum.TURBO;

@Component
public class ChatService implements ChatFunction {

    @Resource(name = "chatRedisTemplate")
    private ReactiveRedisTemplate lambOpenAiChatRedisTemplate;

    @Override
    public Mono<CurrentConversation<ChatMessageResponse>> execute(ChatParam param) {
        //参数校验
        param.verify();

        String uniqueId = Contract.lambOpenAiUniqueId(param.getUserId(),param.getUniqueParam().getUniqueTime());

        return ReactiveRedisOperation.build(lambOpenAiChatRedisTemplate).get(uniqueId)
                .onErrorResume(e->Mono.error(new EventException(EAI00000007)))
                .defaultIfEmpty(Mono.empty())
                .flatMap(e->{
                    List<ChatMessage> chatMessages = null;
                    List<ChatMessageResponse> chatMessagesResponse = null;
                    List<Conversation<ChatMessageResponse>> conversationList = null;
                    Conversations<ChatMessageResponse> lambOpenAiConversations = null;
                    try {
                        Integer tokens = 0;
                        if(e.equals(Mono.empty())){
                            //没有历史聊天记录,第一次对话,装载AI人设
                            chatMessages = new LinkedList<>();
                            chatMessagesResponse = new LinkedList<>();
                            if(StringUtils.isNotBlank(param.getPersona())){
                                chatMessages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),param.getPersona()));
                                chatMessagesResponse.add(new ChatMessageResponse(ChatMessageRole.SYSTEM.value(),param.getPersona(),currentTime()));
                                tokens = tokens + encoding(param.getPersona());
                            }
                            chatMessages.add(new ChatMessage(ChatMessageRole.USER.value(),param.getPrompt()));
                            chatMessagesResponse.add(new ChatMessageResponse(ChatMessageRole.USER.value(),param.getPrompt(),currentTime()));
                            tokens = tokens + encoding(param.getPrompt());
                            conversationList = new LinkedList<>();
                            Conversation<ChatMessageResponse> conversation = new Conversation<ChatMessageResponse>();
                            conversation.setConversation(chatMessagesResponse);
                            conversationList.add(conversation);
                            lambOpenAiConversations = new Conversations<ChatMessageResponse>();
                            lambOpenAiConversations.setConversations(conversationList);

                            if(!limitVerify(param.getQuota(),param.getMaxTokens(),tokens))return Mono.error(new EventException(EAI00000100));
                        }else {
                            List<ChatMessage> _chatMessages = new LinkedList<>();
                            lambOpenAiConversations = new ObjectMapper().convertValue(e, new TypeReference<>(){});
                            lambOpenAiConversations.getConversations().forEach(conversations->{
                                conversations.getConversation().forEach(messages->{
                                    _chatMessages.add(new ChatMessage(messages.getRole(),messages.getContent()));
                                });
                            });
                            chatMessages = _chatMessages;
                            chatMessages.add(new ChatMessage(ChatMessageRole.USER.value(),param.getPrompt()));
                            chatMessagesResponse = new LinkedList<>();
                            chatMessagesResponse.add(new ChatMessageResponse(ChatMessageRole.USER.value(),param.getPrompt(),currentTime()));
                            tokens = tokens + encoding(param.getPrompt());
                            Conversation<ChatMessageResponse> conversation = new Conversation<>();
                            conversation.setConversation(chatMessagesResponse);
                            lambOpenAiConversations.getConversations().add(conversation);
                            //多轮对话要计算所有的tokens
                            tokens = Math.toIntExact(tokens + lambOpenAiConversations.getTotalTokens());
                            if(!limitVerify(param.getQuota(),param.getMaxTokens(),tokens))return Mono.error(new EventException(EAI00000100));
                        }
                    }catch (Throwable throwable){
                        return Mono.error(new EventException(EAI00000006,throwable.getMessage()));
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
                        Conversation<ChatMessageResponse> conversation = lambOpenAiConversations.getConversations().get(lambOpenAiConversations.getConversations().size()-1);
                        conversation.setPromptTokens(chatCompletionResult.getUsage().getPromptTokens());
                        conversation.setCompletionTokens(chatCompletionResult.getUsage().getCompletionTokens());
                        conversation.setTotalTokens(chatCompletionResult.getUsage().getTotalTokens());
                        conversation.getConversation().add(new ChatMessageResponse(chatMessage.getRole(),chatMessage.getContent(),Contract.currentTime()));

                        lambOpenAiConversations.setTotalTokens(lambOpenAiConversations.getTotalTokens() + chatCompletionResult.getUsage().getTotalTokens());
                        lambOpenAiConversations.setTotalPromptTokens(lambOpenAiConversations.getTotalPromptTokens() + chatCompletionResult.getUsage().getPromptTokens());
                        lambOpenAiConversations.setTotalCompletionTokens(lambOpenAiConversations.getTotalCompletionTokens() + chatCompletionResult.getUsage().getCompletionTokens());

                        ReactiveRedisOperation.build(lambOpenAiChatRedisTemplate).set(uniqueId,lambOpenAiConversations);

                        return Mono.just(conversation).flatMap(current->{
                            CurrentConversation<ChatMessageResponse> currentConversation =  new CurrentConversation<ChatMessageResponse>();
                            currentConversation.setCurrentConversation(current.getConversation().get(current.getConversation().size()-1));
                            currentConversation.setCurrentPromptTokens(current.getPromptTokens());
                            currentConversation.setCurrentCompletionTokens(current.getCompletionTokens());
                            currentConversation.setCurrentTotalTokens(current.getTotalTokens());
                            return Mono.just(currentConversation);
                        });
                    }catch (Throwable throwable){
                        return Mono.error(new EventException(EAI00000006,throwable.getMessage()));
                    }
                });
    }

}
