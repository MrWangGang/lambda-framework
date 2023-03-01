package org.lamb.framework.common.util.sample;

import org.lamb.framework.common.exception.LambEventException;

import java.util.Map;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;

/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public class MapGetUtil {
    public static <T>T get(Map map, String value){
        if(StringUtil.isBlank(value)){
            throw new LambEventException(ES00000002);
        }

        if(map == null){
            throw new LambEventException(ES00000001);
        }

        if(map.isEmpty()){
            throw new LambEventException(ES00000001);
        }

        Object obj = map.get(value);
        if(obj == null){
            throw new LambEventException(ES00000003);
        }
        T t = (T)obj ;
        if(t == null){
            throw new LambEventException(ES00000003);
        }

        return t;
    }
}
