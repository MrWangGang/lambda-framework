package org.lambda.framework.sub.openai.service.chat.response;

import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatReplied extends ChatMessage {

    private String time;

    public ChatReplied(String role, String content, String time) {
       super.setRole(role);
       super.setContent(content);
       this.time = time;
    }
}
