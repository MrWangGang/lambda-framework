package org.lamb.framework.sub.openai;

import org.lamb.framework.common.util.sample.MD5Util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LambOpenAiContract {
    public static final Long clientTimeOut = 60L;
    public static  String currentTime(){
        long currentTime = System.currentTimeMillis();
        Date date = new Date(currentTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }
    public static LambOpenAiUniqueParam lambOpenAiUniqueId(String userId){
        long currentTime = System.currentTimeMillis();
        Date date = new Date(currentTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd&HH:mm:ss");
        String uniquetime = dateFormat.format(date);
        LambOpenAiUniqueParam lambOpenAiUniqueParam = LambOpenAiUniqueParam.builder()
                .uniqueTime(uniquetime)
                .uniqueId(lambOpenAiMD5UniqueId(userId,uniquetime))
                .build();
        return lambOpenAiUniqueParam;
    }

    public static String lambOpenAiUniqueId(String userId,String uniquetime){

        return "&"+userId+"&"+ MD5Util.hash(userId+"@"+uniquetime) + "&"+uniquetime;
    }

    public static String lambOpenAiMD5UniqueId(String userId,String uniquetime){
        return MD5Util.hash(lambOpenAiUniqueId(userId,uniquetime));
    }

    public static boolean verify(String userId, LambOpenAiUniqueParam lambOpenAiUniqueParam){
        if(!lambOpenAiMD5UniqueId(userId, lambOpenAiUniqueParam.getUniqueTime()).equals(lambOpenAiUniqueParam.getUniqueId()))return false;
        return true;
    }


}
