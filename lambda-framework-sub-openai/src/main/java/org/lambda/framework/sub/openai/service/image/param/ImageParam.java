package org.lambda.framework.sub.openai.service.image.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.sub.openai.AbstractParam;
import org.lambda.framework.sub.openai.UniqueParam;

import static org.lambda.framework.common.enums.ExceptionEnum.*;
import static org.lambda.framework.sub.openai.Contract.*;

@Getter
@Setter
public class ImageParam extends AbstractParam {


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
    public ImageParam(Integer n, String prompt, Long quota, Integer maxTokens, Long timeOut, String openAiApiKey, String userId, UniqueParam uniqueParam, String size, String responseFormat, String user) {
        super(n, prompt, quota, maxTokens, timeOut, openAiApiKey, userId, uniqueParam);
        this.size = size;
        this.responseFormat = responseFormat;
        this.user = user;
    }
}
