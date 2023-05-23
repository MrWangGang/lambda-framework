package org.lambda.framework.openai.service.image.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.openai.OpenAiAbstractParam;
import org.lambda.framework.openai.OpenAiUniqueParam;
import org.lambda.framework.openai.enums.OpenaiExceptionEnum;

import static org.lambda.framework.openai.OpenAiContract.*;

@Getter
@Setter
public class OpenAiImageParam extends OpenAiAbstractParam {


    public void verify(){
        super.verify();
        if(StringUtils.isBlank(responseFormat))throw new EventException(OpenaiExceptionEnum.ES_OPENAI_009);
        if(StringUtils.isBlank(size))throw new EventException(OpenaiExceptionEnum.ES_OPENAI_011);
        switch (size){
            case image_size_256:break;
            case image_size_512:break;
            case image_size_1024:break;
            default:throw new EventException(OpenaiExceptionEnum.ES_OPENAI_010);
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
