package org.lambda.framework.repository.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum RepositoryExceptionEnum implements ExceptionEnumFunction {

    ES_REPOSITORY_MYSQL_000("ES_REPOSITORY_MYSQL_000","mysql缺少host配置"),
    ES_REPOSITORY_MYSQL_001("ES_REPOSITORY_MYSQL_001","mysql缺少user配置"),
    ES_REPOSITORY_MYSQL_002("ES_REPOSITORY_MYSQL_002","mysql缺少password配置"),
    ES_REPOSITORY_MYSQL_003("ES_REPOSITORY_MYSQL_003","mysql缺少database配置"),

    ES_REPOSITORY_MONGO_004("ES_REPOSITORY_MONGO_004","mongo缺少host配置"),
    ES_REPOSITORY_MONGO_005("ES_REPOSITORY_MONGO_005","mongo缺少user配置"),
    ES_REPOSITORY_MONGO_006("ES_REPOSITORY_MONGO_006","mongo缺少password配置"),
    ES_REPOSITORY_MONGO_007("ES_REPOSITORY_MONGO_007","mongo缺少database配置"),
    ES_REPOSITORY_MONGO_008("ES_REPOSITORY_MONGO_008","mongo缺少auth-database配置"),
    ES_REPOSITORY_MONGO_009("ES_REPOSITORY_MONGO_009","mongo缺少replica"),
    ES_REPOSITORY_MONGO_010("ES_REPOSITORY_MONGO_010","mongo缺少deploy"),
    ES_REPOSITORY_MONGO_011("ES_REPOSITORY_MONGO_011","mongo缺少connect-timeout-seconds"),
    ES_REPOSITORY_MONGO_012("ES_REPOSITORY_MONGO_012","mongo缺少max-idle-time-seconds"),
    ES_REPOSITORY_MONGO_013("ES_REPOSITORY_MONGO_013","mongo缺少max-size"),
    ES_REPOSITORY_MONGO_014("ES_REPOSITORY_MONGO_014","无效的mongo部署模式"),



    ES_REPOSITORY_100("ES_REPOSITORY_100","分页参数不规范 page 必须 >0 size必须 >0 "),
    ES_REPOSITORY_101("ES_REPOSITORY_101","对mongo的密码编码失败"),
    ES_REPOSITORY_102("ES_REPOSITORY_102","分页页码初始值不能小于1，必须从1开始"),
    ES_REPOSITORY_103("ES_REPOSITORY_103","分页参数缺失"),
    ES_REPOSITORY_104("ES_REPOSITORY_104","当前页参数缺失"),
    ES_REPOSITORY_105("ES_REPOSITORY_105","每页条数参数缺失"),
    ES_REPOSITORY_106("ES_REPOSITORY_106","对枚举类型转换失败");





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
