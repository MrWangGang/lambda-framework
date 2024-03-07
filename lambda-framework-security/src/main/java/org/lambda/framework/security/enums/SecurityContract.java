package org.lambda.framework.security.enums;

/**
 * @description: 契约
 * @author: Mr.WangGang
 * @create: 2018-11-16 下午 3:56
 **/
public class SecurityContract {


    public static final String LAMBDA_SECURITY_EMPTY_STR = "";

    //所有经过认证的URL都需要经过授权
    public static final String LAMBDA_SECURITY_URL_AUTZ_MODEL_ALL="ALL";

    //只有配置映射的认证的URL都需要授权
    public static final String LAMBDA_SECURITY_URL_AUTZ_MODEL_MAPPING="MAPPING";


}
