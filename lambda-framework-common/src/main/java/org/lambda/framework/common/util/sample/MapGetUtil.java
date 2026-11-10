package org.lambda.framework.common.util.sample;


import org.lambda.framework.common.exception.EventException;

import java.util.Map;

import static org.lambda.framework.enums.CommonExceptionEnum.*;


/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public class MapGetUtil {
    public static <T>T get(Map map, String value){
        if(StringUtil.isBlank(value)){
            throw new EventException(MAP_KEY_MISSING);
        }

        if(map == null){
            throw new EventException(MAP_EMPTY);
        }

        if(map.isEmpty()){
            throw new EventException(MAP_EMPTY);
        }

        Object obj = map.get(value);
        if(obj == null){
            throw new EventException(MAP_VALUE_MISSING);
        }
        T t = (T)obj ;
        if(t == null){
            throw new EventException(MAP_VALUE_MISSING);
        }

        return t;
    }
}
