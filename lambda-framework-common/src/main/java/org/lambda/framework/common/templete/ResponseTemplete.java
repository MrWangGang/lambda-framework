package org.lambda.framework.common.templete;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.lambda.framework.common.enums.JsonSymbolicFinalConfig;

import java.io.Serializable;

/**
 * Created by WangGang on 2017/7/4 0004.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
@Data
public class ResponseTemplete implements Serializable {

    private String serviceCode = JsonSymbolicFinalConfig.DEFAULT_SUCCESS_SERVICE_CODE;

    private String serviceMessage = JsonSymbolicFinalConfig.DEFAULT_SUCCESS_SERVICE_MESSAGE;;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;

    public ResponseTemplete(Object data){
        this.data = data;
    }

    public ResponseTemplete(){

    }
}
