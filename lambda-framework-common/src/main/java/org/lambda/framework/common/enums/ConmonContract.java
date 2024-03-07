package org.lambda.framework.common.enums;

public class ConmonContract {
    //rsocket 元数据里只能用小写
    public static final String PRINCIPAL_STASH_NAMING  = "lambda/principal-stash";
    public static final String AUTHTOKEN_STASH_NAMING = "lambda/auth-token";
    public static final String AUTH_TOKEN_NAMING = "Auth-Token";
    public static final String LAMBDA_SECURITY_AUTH_TOKEN_KEY = "lambda.security.auth-token.";
    public static final String LAMBDA_SECURITY_AUTH_TOKEN_REGEX = LAMBDA_SECURITY_AUTH_TOKEN_KEY+"[a-zA-Z\\d]+\\.[a-zA-Z\\d]+";
    public static final String TOKEN_SUFFIX = "prefix";
    public static final String LAMBDA_SECURITY_AUTH_TOKEN_SALT  = "QWERTYUIOPasdfghjklZXCVBNM<>?";
    public static final Long LAMBDA_SECURITY_TOKEN_TIME_SECOND = 2592000L;

}
