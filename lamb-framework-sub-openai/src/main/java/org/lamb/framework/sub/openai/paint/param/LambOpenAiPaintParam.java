package org.lamb.framework.sub.openai.paint.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.lamb.framework.sub.openai.LambOpenAiParam;
import org.lamb.framework.sub.openai.LambOpenAiUniqueParam;

@Getter
@Setter
public class LambOpenAiPaintParam extends LambOpenAiParam {
    //提示
    private String prompt;
    //需要生成的图片数量
    private Integer n;
    //图片大小 256 512 1024
    private String size;
    private String responseFormat;
    private String user;

    @Builder
    public LambOpenAiPaintParam(Long timeOut, String openAiApiKey, String userId, LambOpenAiUniqueParam lambOpenAiUniqueParam, String prompt, Integer n, String size, String responseFormat, String user) {
        super(timeOut, openAiApiKey, userId, lambOpenAiUniqueParam);
        this.prompt = prompt;
        this.n = n;
        this.size = size;
        this.responseFormat = responseFormat;
        this.user = user;
    }
}
