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
    ES_REPOSITORY_MONGO_015("ES_REPOSITORY_MONGO_015","change stream 监听发生错误"),
    ES_REPOSITORY_MONGO_016("ES_REPOSITORY_MONGO_016","change stream 监听配置失败,初始信息不能为空"),
    ES_REPOSITORY_MONGO_017("ES_REPOSITORY_MONGO_017","ID类型不支持"),
    ES_REPOSITORY_MONGO_018("ES_REPOSITORY_MONGO_018","PO没有定义文档名称"),
    ES_REPOSITORY_MONGO_019("ES_REPOSITORY_MONGO_019","PO缺少文档名称"),


    ES_REPOSITORY_ELASTICSEARCH_020("ES_REPOSITORY_ELASTICSEARCH_020","elasticsearch缺少host"),
    ES_REPOSITORY_ELASTICSEARCH_021("ES_REPOSITORY_ELASTICSEARCH_021","elasticsearch缺少user"),
    ES_REPOSITORY_ELASTICSEARCH_022("ES_REPOSITORY_ELASTICSEARCH_022","elasticsearch缺少password"),
    ES_REPOSITORY_ELASTICSEARCH_023("ES_REPOSITORY_ELASTICSEARCH_023","elasticsearch缺少connectTimeoutSeconds"),
    ES_REPOSITORY_ELASTICSEARCH_024("ES_REPOSITORY_ELASTICSEARCH_024","elasticsearch缺少socketTimeoutSeconds"),

    ES_REPOSITORY_REDIS_030("ES_REPOSITORY_REDIS_030","redis host缺失"),
    ES_REPOSITORY_REDIS_031("ES_REPOSITORY_REDIS_031","redis port缺失"),
    ES_REPOSITORY_REDIS_032("ES_REPOSITORY_REDIS_032","redis maxActive缺失"),
    ES_REPOSITORY_REDIS_033("ES_REPOSITORY_REDIS_033","redis maxWaitSeconds缺失"),
    ES_REPOSITORY_REDIS_034("ES_REPOSITORY_REDIS_034","redis maxIdle缺失"),
    ES_REPOSITORY_REDIS_035("ES_REPOSITORY_REDIS_035","redis minIdle缺失"),
    ES_REPOSITORY_REDIS_036("ES_REPOSITORY_REDIS_036","redis database缺失"),
    ES_REPOSITORY_REDIS_037("ES_REPOSITORY_REDIS_037","redis depolyModel不能缺失"),
    ES_REPOSITORY_REDIS_039("ES_REPOSITORY_REDIS_039","redis 哨兵模式必须有masterName"),
    ES_REPOSITORY_REDIS_040("ES_REPOSITORY_REDIS_040","无效的redis部署模式"),

    ES_REPOSITORY_100("ES_REPOSITORY_100","分页参数不规范 page 必须 >0 size必须 >0 "),
    ES_REPOSITORY_101("ES_REPOSITORY_101","对mongo的密码编码失败"),
    ES_REPOSITORY_102("ES_REPOSITORY_102","分页页码初始值不能小于1，必须从1开始"),
    ES_REPOSITORY_103("ES_REPOSITORY_103","分页参数缺失"),
    ES_REPOSITORY_104("ES_REPOSITORY_104","当前页参数缺失"),
    ES_REPOSITORY_105("ES_REPOSITORY_105","每页条数参数缺失"),
    ES_REPOSITORY_106("ES_REPOSITORY_106","对枚举类型转换失败"),
    ES_REPOSITORY_107("ES_REPOSITORY_107","无效的change stream operation type枚举类型"),
    ES_REPOSITORY_108("ES_REPOSITORY_108","找不到对应的repository配置类"),
    ES_REPOSITORY_109("ES_REPOSITORY_109","convert结果为null");




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
