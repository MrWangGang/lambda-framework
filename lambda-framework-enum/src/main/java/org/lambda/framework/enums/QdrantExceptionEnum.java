package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum QdrantExceptionEnum implements ExceptionEnumFunction {
    COLLECTION_NOT_READY("S1200001", "向量查询暂不可用，请稍后重试");

    private final String code;
    private final String message;

    QdrantExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
