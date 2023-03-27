package org.lamb.framework.sub.openai;

import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LambOpenAiMessage extends ChatMessage {
    private String time;

    public LambOpenAiMessage(String role, String content, String time) {
       super.setRole(role);
       super.setContent(content);
       this.time = time;
    }
}
