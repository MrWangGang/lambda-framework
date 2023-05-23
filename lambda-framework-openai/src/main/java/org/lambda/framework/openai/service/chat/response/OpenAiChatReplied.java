package org.lambda.framework.openai.service.chat.response;

import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OpenAiChatReplied extends ChatMessage {

    private String time;

    public OpenAiChatReplied(String role, String content, String time) {
       super.setRole(role);
       super.setContent(content);
       this.time = time;
    }
}
