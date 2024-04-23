package org.lambda.framework.rsocket.adapter;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.stereotype.Component;

import static org.lambda.framework.common.exception.basic.GlobalException.EX_PRIEX;
import static org.lambda.framework.rsocket.enums.RsocketExceptionEnum.ES_RSOCKET_000;
import static org.lambda.framework.rsocket.enums.RsocketExceptionEnum.ES_RSOCKET_001;

@Component
public class RsocketGlobalExceptionHandler  {
    public  Throwable handleException(Throwable e) {
        // 处理异常并返回适当的响应
        GlobalException globalException = null;
        if(e instanceof GlobalException){
            globalException =   (GlobalException) e;
        }else if (e instanceof MethodArgumentResolutionException) {
            // 如果是方法参数解析异常，可能是客户端请求参数错误导致的，返回参数错误的信息
            globalException =   new EventException(ES_RSOCKET_001);
        }else {
            if(StringUtils.isBlank(e.getMessage())){
                globalException = new GlobalException(ES_RSOCKET_000.getCode(), ES_RSOCKET_000.getMessage());
            }
            // 找到第一个冒号的位置
            int index = e.getMessage().indexOf(EX_PRIEX);
            if (index != -1) {
                // 获取第一个冒号前面的字符串
                String firstPart = e.getMessage().substring(0, index);
                // 获取第一个冒号后面的字符串
                String secondPart = e.getMessage().substring(index + 1);
                globalException = new GlobalException(firstPart,secondPart);
            } else {
                // 如果没有找到冒号，则输出原始错误信息
                globalException = new GlobalException(ES_RSOCKET_000.getCode(),e.getMessage());
            }
        }
        return new Throwable(globalException.getSerializeMessage());
    }
}
