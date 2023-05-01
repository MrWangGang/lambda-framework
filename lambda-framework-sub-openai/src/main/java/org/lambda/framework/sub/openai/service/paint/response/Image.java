package org.lambda.framework.sub.openai.service.paint.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    private String time;

    private String prompt;

    private List<com.theokanning.openai.image.Image> images;

    @Builder
    public Image(List<com.theokanning.openai.image.Image> images, String prompt, String time) {
        this.time = time;
        this.prompt = prompt;
        this.images = images;
    }
}
