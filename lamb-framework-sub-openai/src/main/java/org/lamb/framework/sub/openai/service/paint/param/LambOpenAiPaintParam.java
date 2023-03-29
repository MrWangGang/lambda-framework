package org.lamb.framework.sub.openai.service.paint.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.sub.openai.LambOpenAiContract;
import org.lamb.framework.sub.openai.LambOpenAiParam;
import org.lamb.framework.sub.openai.LambOpenAiUniqueParam;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;
import static org.lamb.framework.sub.openai.LambOpenAiContract.*;

@Getter
@Setter
public class LambOpenAiPaintParam extends LambOpenAiParam {


    public void verify(){
        super.verify();
        if(StringUtils.isBlank(responseFormat))throw new LambEventException(EAI0000010);
        if(StringUtils.isBlank(size))throw new LambEventException(EAI0000012);
        switch (size){
            case image_size_256:break;
            case image_size_512:break;
            case image_size_1024:break;
            default:throw new LambEventException(EAI0000011);
        }
    }

    //图片大小 256 512 1024
    private String size;

    private String responseFormat;
    private String user;

    @Builder
    public LambOpenAiPaintParam(Integer n, String prompt, Long quota, Integer maxTokens, Long timeOut, String openAiApiKey, String userId, LambOpenAiUniqueParam lambOpenAiUniqueParam, String size, String responseFormat, String user) {
        super(n, prompt, quota, maxTokens, timeOut, openAiApiKey, userId, lambOpenAiUniqueParam);
        this.size = size;
        this.responseFormat = responseFormat;
        this.user = user;
    }
}
