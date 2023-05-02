package org.lambda.framework.sub.openai;

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
