package org.lambda.framework.repository.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum RepositoryExceptionEnum implements ExceptionEnumFunction {

    ES_REPOSITORY_MYSQL_000("ES_REPOSITORY_MYSQL_000","mysql缺少host配置"),
    ES_REPOSITORY_MYSQL_001("ES_REPOSITORY_MYSQL_001","mysql缺少user配置"),
    ES_REPOSITORY_MYSQL_002("ES_REPOSITORY_MYSQL_002","mysql缺少password配置"),
    ES_REPOSITORY_MYSQL_003("ES_REPOSITORY_MYSQL_003","mysql缺少database配置"),

    ES_REPOSITORY_MYSQL_100("ES_REPOSITORY_MYSQL_100","分页参数不规范 page 必须 >0 size必须 >0 ");


    private String code;

    private String message;
    // 构造方法
    private RepositoryExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
