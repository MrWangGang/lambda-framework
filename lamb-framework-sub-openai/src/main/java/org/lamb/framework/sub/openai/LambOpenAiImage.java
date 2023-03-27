package org.lamb.framework.sub.openai;

import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.image.Image;
import lombok.*;
import org.checkerframework.checker.units.qual.A;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LambOpenAiImage {
    private String time;

    private String prompt;

    private List<Image> images;

    @Builder
    public LambOpenAiImage(List<Image> images,String prompt,String time) {
        this.time = time;
        this.prompt = prompt;
        this.images = images;
    }
}
