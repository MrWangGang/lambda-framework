package org.lambda.framework.web.security.contract;

/**
 * @description: 契约
 * @author: Mr.WangGang
 * @create: 2018-11-16 下午 3:56
 **/
public class Contract {
    public static final String LAMBDA_SECURITY_AUTH_TOKEN_SALT  = "lambda.salt";
    public static final String LAMBDA_SECURITY_AUTH_TOKEN_KEY = "lambda.auth.token.";
    public static final Long LAMBDA_SECURITY_TOKEN_TIME_SECOND = Long.valueOf(600);
    public static final String LAMBDA_SECURITY_AUTH_TOKEN_REGX = "^"+LAMBDA_SECURITY_AUTH_TOKEN_KEY+"[a-zA-Z\\d]{1,}";

    public static final String LAMBDA_SECURITY_EMPTY_STR = "";

    //所有经过认证的URL都需要经过授权
    public static final String LAMBDA_SECURITY_URL_AUTZ_ALL="ALL";

    //只有配置映射的认证的URL都需要授权
    public static final String LAMBDA_SECURITY_URL_AUTZ_MAPPING="MAPPING";

    public static String AUTH_TOKEN_NAMING = "Auth-Token";

}
