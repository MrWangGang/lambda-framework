package org.lambda.framework.sub.openai.service.image.response;

import com.theokanning.openai.image.Image;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageReplied {
    private String time;

    private String prompt;

    private List<Image> images;

    @Builder
    public ImageReplied(List<Image> images, String prompt, String time) {
        this.time = time;
        this.prompt = prompt;
        this.images = images;
    }
}
