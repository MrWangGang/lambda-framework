package org.lambda.framework.guid.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum GuidExceptionEnum implements ExceptionEnumFunction {

    //系统异常-guid 异常 - 100-199
    ES_GUID_000("ES_GUID_000","时钟回拨,GUID算法无法生成新的ID"),
    ES_GUID_001("ES_GUID_001","GUID生成新的ID发生未知异常"),
    ES_GUID_002("ES_GUID_002","GUIDFactory->必须指定datacenterId"),
    ES_GUID_003("ES_GUID_003","GUIDFactory->必须指定machineId");
    // 成员变量
    private String code;

    private String message;
    // 构造方法
    private GuidExceptionEnum(String code, String message) {
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
