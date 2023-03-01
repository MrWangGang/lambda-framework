package org.lamb.framework.common.util.sample;

import org.lamb.framework.common.exception.LambEventException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import static org.lamb.framework.common.enums.LambExceptionEnum.ES00000005;

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
            throw new LambEventException(ES00000005);
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
