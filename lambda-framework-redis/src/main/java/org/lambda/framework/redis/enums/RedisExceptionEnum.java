package org.lambda.framework.redis.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum RedisExceptionEnum implements ExceptionEnumFunction {

    ES_REDIS_000("ES_REDIS_000","redis host缺失"),
    ES_REDIS_001("ES_REDIS_001","redis port缺失"),
    ES_REDIS_002("ES_REDIS_002","redis maxActive缺失"),
    ES_REDIS_003("ES_REDIS_003","redis maxWaitSeconds缺失"),
    ES_REDIS_004("ES_REDIS_004","redis maxIdle缺失"),
    ES_REDIS_005("ES_REDIS_005","redis minIdle缺失"),
    ES_REDIS_006("ES_REDIS_006","redis database缺失"),
    ES_REDIS_020("ES_REDIS_020","redis 部署模型不能缺失"),
    ES_REDIS_021("ES_REDIS_021","redis 无效的redis部署模式"),
    ES_REDIS_022("ES_REDIS_022","redis 哨兵模式必须有masterName");


    private String code;

    private String message;
    // 构造方法
    private RedisExceptionEnum(String code, String message) {
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
