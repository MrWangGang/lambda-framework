package org.lambda.framework.openai;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiUniqueParam {
    private String uniqueId;

    private String uniqueTime;
}
