package org.lambda.framework.common.util.sample;


import org.lambda.framework.common.exception.EventException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_004;


/**
 * Created by WangGang on 2017/7/6 0006.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public class URLEncoderUtil {

    public static String encode(String value){
        try {
            return URLEncoder.encode(value,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new EventException(ES_COMMON_004);
        }
    }

    public static String conver(String url,Map<String,Object> map){
        if(map == null){
            return "";
        }
        if(map.isEmpty()){
            return "";
        }

        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue().toString());
            sb.append("&");
        }
        String s = sb.toString();
        if (s.endsWith("&")) {
            s = org.apache.commons.lang3.StringUtils.substringBeforeLast(s, "&");
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(url).append("?").append(s);
        return buffer.toString();
    }
}
