package org.lambda.framework.common.enums;

/**
 * @description: 契约
 * @author: Mr.WangGang
 * @create: 2018-11-16 下午 3:56
 **/
public class SecurityContract {

    public static final String LAMBDA_SECURITY_AUTH_TOKEN_SALT  = "QWERTYUIOPasdfghjklZXCVBNM<>?";
    public static final Long LAMBDA_SECURITY_TOKEN_TIME_SECOND = 2592000L;
    public static final String LAMBDA_SECURITY_AUTH_TOKEN_KEY = "lambda.security.auth-token.";
    public static final String LAMBDA_SECURITY_AUTH_TOKEN_REGEX = LAMBDA_SECURITY_AUTH_TOKEN_KEY+"[a-zA-Z\\d]+\\.[a-zA-Z\\d]+";
    public static final String TOKEN_SUFFIX = "prefix";
    public static final String LAMBDA_SECURITY_EMPTY_STR = "";
    public static final String LAMBDA_EMPTY_PRINCIPAL = "none-principal";

    //所有经过认证的URL都需要经过授权
    public static final String LAMBDA_SECURITY_URL_AUTZ_MODEL_ALL="ALL";

    //只有配置映射的认证的URL都需要授权
    public static final String LAMBDA_SECURITY_URL_AUTZ_MODEL_MAPPING="MAPPING";

    public static String AUTH_TOKEN_NAMING = "Auth-Token";

    public static final String PRINCIPAL_STASH_NAMING  = "message/x.rsocket.routing.v0";


}
