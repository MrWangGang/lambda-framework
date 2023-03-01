package org.lamb.framework.common.templete;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.lamb.framework.common.enums.LambJsonSymbolicFinalConfig;

import java.io.Serializable;

/**
 * Created by WangGang on 2017/7/4 0004.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
@Data
public class LambResponseTemplete implements Serializable {

    private String serviceCode = LambJsonSymbolicFinalConfig.DEFAULT_SUCCESS_SERVICE_CODE;

    private String serviceMessage = LambJsonSymbolicFinalConfig.DEFAULT_SUCCESS_SERVICE_MESSAGE;;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;

    public LambResponseTemplete(Object data){
        this.data = data;
    }

    public LambResponseTemplete(){

    }
}
