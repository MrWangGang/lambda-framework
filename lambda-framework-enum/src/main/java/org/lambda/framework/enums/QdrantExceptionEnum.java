package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum QdrantExceptionEnum implements ExceptionEnumFunction {
    COLLECTION_NOT_READY("S1200001", "向量查询暂不可用，请稍后重试"),
    CONFIGURATION_INVALID("S1200002", "向量组件配置不正确"),
    REQUEST_FAILED("S1200003", "向量服务请求失败，请稍后重试");

    private final String code;
    private final String message;

    QdrantExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
