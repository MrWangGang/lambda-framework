package org.lamb.framework.sub.openai;

import org.apache.commons.lang3.time.DateUtils;
import org.lamb.framework.common.util.sample.MD5Util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LambOpenAiContract {

    public static final Long clientTimeOut = 60L;
    public static String lambOpenAiUniqueId(String userId){
        long currentTime = System.currentTimeMillis();
        Date date = new Date(currentTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd&HH:mm:ss");
        String dateStr = dateFormat.format(date);
        return userId+"&"+ MD5Util.hash(userId+"@"+dateStr) + "&"+dateStr;
    }

    public static boolean verify(String userId,String chatId){
        String[] chatIds= chatId.split("&");
        String salt = MD5Util.hash(userId+"@"+chatIds[2]+"&"+chatIds[3]);
        if(userId.equals(chatIds[0]) && chatIds[1].equals(salt)){
            return true;
        }
        return false;
    }


}
