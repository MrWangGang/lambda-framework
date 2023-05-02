package org.lambda.framework.sub.openai.service.image.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.sub.openai.OpenAiAbstractParam;
import org.lambda.framework.sub.openai.OpenAiUniqueParam;

import static org.lambda.framework.common.enums.ExceptionEnum.*;
import static org.lambda.framework.sub.openai.OpenAiContract.*;

@Getter
@Setter
public class OpenAiImageParam extends OpenAiAbstractParam {


    public void verify(){
        super.verify();
        if(StringUtils.isBlank(responseFormat))throw new EventException(EAI0000010);
        if(StringUtils.isBlank(size))throw new EventException(EAI0000012);
        switch (size){
            case image_size_256:break;
            case image_size_512:break;
            case image_size_1024:break;
            default:throw new EventException(EAI0000011);
        }
    }

    //图片大小 256 512 1024
    private String size;

    private String responseFormat;
    private String user;

    @Builder
    public OpenAiImageParam(Integer n, String prompt, Long quota, Integer maxTokens, Long timeOut, String apiKey, String userId, OpenAiUniqueParam uniqueParam, String size, String responseFormat, String user) {
        super(n, prompt, quota, maxTokens, timeOut, apiKey, userId, uniqueParam);
        this.size = size;
        this.responseFormat = responseFormat;
        this.user = user;
    }
}
