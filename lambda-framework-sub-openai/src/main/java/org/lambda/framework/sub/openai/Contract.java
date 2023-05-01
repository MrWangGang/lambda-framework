package org.lambda.framework.sub.openai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.common.util.sample.MD5Util;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.lamb.framework.common.enums.LambExceptionEnum.EAI0000011;

public class Contract {
    public static final Long clientTimeOut = 60L;

    public static final String responseFormat = "url";

    public static final String image_size_256 = "256x256";
    public static final String image_size_512 = "512x512";
    public static final String image_size_1024 = "1024x1024";

    public static Integer imageTokens(String size,Integer n){
        int i = 0;
        switch (size){
            case image_size_256:i = 8000;break;
            case image_size_512:i= 9000;break;
            case image_size_1024:i = 100000;break;
            default:throw new LambEventException(EAI0000011);
        }
        return i * n;
    }

    public static Integer encoding(String propmt){
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding enc = registry.getEncodingForModel(ModelType.GPT_3_5_TURBO);
        return enc.encode(propmt).size();
    }

    public static  String currentTime(){
        long currentTime = System.currentTimeMillis();
        Date date = new Date(currentTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }
    public static org.lamb.framework.sub.openai.LambOpenAiUniqueParam lambOpenAiUniqueId(String userId){
        long currentTime = System.currentTimeMillis();
        Date date = new Date(currentTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd&HH:mm:ss");
        String uniquetime = dateFormat.format(date);
        org.lamb.framework.sub.openai.LambOpenAiUniqueParam lambOpenAiUniqueParam = org.lamb.framework.sub.openai.LambOpenAiUniqueParam.builder()
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

    public static boolean verify(String userId, org.lamb.framework.sub.openai.LambOpenAiUniqueParam lambOpenAiUniqueParam){
        if(!lambOpenAiMD5UniqueId(userId, lambOpenAiUniqueParam.getUniqueTime()).equals(lambOpenAiUniqueParam.getUniqueId()))return false;
        return true;
    }


}
