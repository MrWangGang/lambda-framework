package org.lamb.framework.sub.openai;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LambOpenAiUniqueParam {
    private String uniqueId;

    private String uniqueTime;
}
