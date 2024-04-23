package org.lambda.framework.web.adapter;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.lambda.framework.common.exception.basic.GlobalException.EX_PRIEX;
import static org.lambda.framework.web.enums.WebExceptionEnum.ES_WEB_000;

@RestControllerAdvice
public class WebGlobalExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public  Throwable handleException(Throwable e) {
        // 处理异常并返回适当的响应
        GlobalException globalException = null;
        if(e instanceof GlobalException){
            globalException =   (GlobalException) e;
        }else {
            if(StringUtils.isBlank(e.getMessage())){
                globalException = new GlobalException(ES_WEB_000.getCode(), ES_WEB_000.getMessage());
            }
            // 找到第一个冒号的位置
            int index = e.getMessage().indexOf(EX_PRIEX);
            if (index != -1) {
                // 获取第一个冒号前面的字符串
                String firstPart = e.getMessage().substring(0, index);
                // 获取第一个冒号后面的字符串
                String secondPart = e.getMessage().substring(index + EX_PRIEX.length());
                globalException = new GlobalException(firstPart,secondPart);
            } else {
                // 如果没有找到冒号，则输出原始错误信息
                globalException = new GlobalException(ES_WEB_000.getCode(),e.getMessage());
            }
        }
        return new Throwable(globalException.getSerializeMessage());
    }
}
