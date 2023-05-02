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
import org.lambda.framework.sub.openai.Replying;
import org.lambda.framework.sub.openai.service.chat.param.FAQParam;
import org.lambda.framework.sub.openai.service.chat.response.ChatReplied;
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
public class FAQService implements FAQServiceFunction {

    @Resource(name = "openAiFAQRedisTemplate")
    private ReactiveRedisTemplate openAiFAQRedisTemplate;
    @Override
    public Mono<Replying<ChatReplied>> execute(FAQParam param) {
        //参数校验
        param.verify();

        String uniqueId = Contract.uniqueId(param.getUserId(),param.getUniqueParam().getUniqueTime());

        return ReactiveRedisOperation.build(openAiFAQRedisTemplate).get(uniqueId)
                .onErrorResume(e->Mono.error(new EventException(EAI00000007)))
                .defaultIfEmpty(Mono.empty())
                .flatMap(e->{
                    List<ChatMessage> chatMessage = null;
                    List<ChatReplied> chatReplied = null;
                    List<Conversation<ChatReplied>> conversation = null;
                    Conversations<ChatReplied> conversations = null;
                    try {
                        Integer tokens = 0;
                        if(e.equals(Mono.empty())){
                            chatMessage = new LinkedList<>();
                            chatReplied = new LinkedList<>();
                            if(StringUtils.isNotBlank(param.getPersona())){
                                chatMessage.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),param.getPersona()));
                                chatReplied.add(new ChatReplied(ChatMessageRole.SYSTEM.value(),param.getPersona(),currentTime()));
                                tokens = tokens + encoding(param.getPersona());
                            }
                            chatMessage.add(new ChatMessage(ChatMessageRole.USER.value(),param.getPrompt()));
                            chatReplied.add(new ChatReplied(ChatMessageRole.USER.value(),param.getPrompt(),currentTime()));
                            tokens = tokens + encoding(param.getPrompt());
                            //没有历史聊天记录,第一次对话,装载AI人设
                            conversation = new LinkedList<>();
                            Conversation<ChatReplied> _conversation = new Conversation<ChatReplied>();
                            _conversation.setConversation(chatReplied);
                            conversation.add(_conversation);
                            conversations = new Conversations<ChatReplied>();
                            conversations.setConversations(conversation);
                            if(!limitVerify(param.getQuota(),param.getMaxTokens(),tokens))return Mono.error(new EventException(EAI00000100));
                        }else {
                            //QA 每次都是新的对话
                            conversations = new ObjectMapper().convertValue(e, new TypeReference<>(){});
                            Conversation<ChatReplied> _conversation = new Conversation<ChatReplied>();
                            chatMessage = new LinkedList();
                            chatReplied = new LinkedList<>();
                            if(StringUtils.isNotBlank(param.getPersona())){
                                chatMessage.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),param.getPersona()));
                                chatReplied.add(new ChatReplied(ChatMessageRole.SYSTEM.value(),param.getPersona(),currentTime()));
                                tokens = tokens + encoding(param.getPersona());
                            }
                            chatMessage.add(new ChatMessage(ChatMessageRole.USER.value(),param.getPrompt()));
                            chatReplied.add(new ChatReplied(ChatMessageRole.USER.value(),param.getPrompt(),currentTime()));
                            tokens = tokens + encoding(param.getPrompt());
                            _conversation.setConversation(chatReplied);
                            conversations.getConversations().add(_conversation);
                            if(!limitVerify(param.getQuota(),param.getMaxTokens(),tokens))return Mono.error(new EventException(EAI00000100));
                        }
                    }catch (Throwable throwable){
                        return Mono.error(new EventException(EAI00000006,throwable.getMessage()));
                    }

                    try {
                        OpenAiService service = new OpenAiService(param.getOpenAiApiKey(),Duration.ofSeconds(param.getTimeOut()));
                        ChatCompletionRequest request = ChatCompletionRequest.builder()
                                .model(TURBO.getModel())
                                .messages(chatMessage)
                                .temperature(param.getTemperature())
                                .topP(param.getTopP())
                                .n(param.getN())
                                .stream(param.getStream())
                                .maxTokens(param.getMaxTokens())
                                .presencePenalty(param.getPresencePenalty())
                                .frequencyPenalty(param.getFrequencyPenalty())
                                .build();

                        ChatCompletionResult chatCompletionResult = service.createChatCompletion(request);
                        ChatMessage _chatMessage = chatCompletionResult.getChoices().get(0).getMessage();
                        Conversation<ChatReplied> _conversation = conversations.getConversations().get(conversations.getConversations().size()-1);
                        _conversation.setPromptTokens(chatCompletionResult.getUsage().getPromptTokens());
                        _conversation.setCompletionTokens(chatCompletionResult.getUsage().getCompletionTokens());
                        _conversation.setTotalTokens(chatCompletionResult.getUsage().getTotalTokens());
                        _conversation.getConversation().add(new ChatReplied(_chatMessage.getRole(),_chatMessage.getContent(),currentTime()));

                        conversations.setTotalTokens(conversations.getTotalTokens() + chatCompletionResult.getUsage().getTotalTokens());
                        conversations.setTotalPromptTokens(conversations.getTotalPromptTokens() + chatCompletionResult.getUsage().getPromptTokens());
                        conversations.setTotalCompletionTokens(conversations.getTotalCompletionTokens() + chatCompletionResult.getUsage().getCompletionTokens());

                        ReactiveRedisOperation.build(openAiFAQRedisTemplate).set(uniqueId,conversations);
                        return Mono.just(_conversation).flatMap(current->{
                            Replying<ChatReplied> currentConversation =  new Replying<ChatReplied>();
                            currentConversation.setReplying(current.getConversation().get(current.getConversation().size()-1));
                            currentConversation.setPromptTokens(current.getPromptTokens());
                            currentConversation.setCompletionTokens(current.getCompletionTokens());
                            currentConversation.setTotalTokens(current.getTotalTokens());
                            return Mono.just(currentConversation);
                        });
                    }catch (Throwable throwable){
                        return Mono.error(new EventException(EAI00000006,throwable.getMessage()));
                    }
                });
    }

}
