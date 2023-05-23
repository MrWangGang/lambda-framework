package org.lambda.framework.openai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.util.sample.MD5Util;
import org.lambda.framework.openai.enums.OpaiExceptionEnum;

import java.text.SimpleDateFormat;
import java.util.Date;


public class OpenAiContract {
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
            default:throw new EventException(OpaiExceptionEnum.ES_OPAI_010);
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
    public static OpenAiUniqueParam uniqueId(String userId){
        long currentTime = System.currentTimeMillis();
        Date date = new Date(currentTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd&HH:mm:ss");
        String uniqueTime = dateFormat.format(date);
        return OpenAiUniqueParam.builder()
                .uniqueTime(uniqueTime)
                .uniqueId(mD5UniqueId(userId,uniqueTime))
                .build();
    }

    public static String uniqueId(String userId,String uniqueTime){

        return "&"+userId+"&"+ MD5Util.hash(userId+"@"+uniqueTime) + "&"+uniqueTime;
    }

    public static String mD5UniqueId(String userId,String uniqueTime){
        return MD5Util.hash(uniqueId(userId,uniqueTime));
    }

    public static boolean verify(String userId, OpenAiUniqueParam openAiUniqueParam){
        if(!mD5UniqueId(userId, openAiUniqueParam.getUniqueTime()).equals(openAiUniqueParam.getUniqueId()))return false;
        return true;
    }


}
