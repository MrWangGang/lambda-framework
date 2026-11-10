package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum CommonExceptionEnum implements ExceptionEnumFunction {
    UNEXPECTED_FAILURE("S0000000", "系统繁忙，请稍后重试"),
    MAP_EMPTY("S0000001", "缺少请求信息"),
    MAP_KEY_MISSING("S0000002", "无法访问所需信息"),
    MAP_VALUE_MISSING("S0000003", "读取信息失败"),
    UNSUPPORTED_TEXT_ENCODING("S0000004", "信息处理失败"),
    CRYPTO_ALGORITHM_UNAVAILABLE("S0000005", "信息处理失败"),
    CRYPTO_PADDING_UNAVAILABLE("S0000006", "信息处理失败"),
    DECRYPTION_INPUT_UNREADABLE("S0000007", "信息处理失败"),
    INVALID_PRIVATE_KEY("S0000008", "信息处理失败"),
    DECRYPTION_PADDING_INVALID("S0000009", "信息处理失败"),
    DECRYPTION_BLOCK_INVALID("S0000010", "信息处理失败"),
    INVALID_PRIVATE_KEY_FORMAT("S0000011", "信息处理失败"),
    ENCRYPTION_INPUT_UNREADABLE("S0000012", "信息处理失败"),
    INVALID_PUBLIC_KEY("S0000013", "信息处理失败"),
    ENCRYPTION_PADDING_INVALID("S0000014", "信息处理失败"),
    INVALID_PUBLIC_KEY_FORMAT("S0000015", "信息处理失败"),
    ENCRYPTION_BLOCK_INVALID("S0000016", "信息处理失败"),
    SIGNATURE_PROCESSING_FAILED("S0000017", "信息处理失败"),
    JSON_READ_FAILED("S0000018", "请求内容格式不正确"),
    JSON_WRITE_FAILED("S0000019", "结果处理失败"),
    TIME_REQUIRED("S0000023", "请填写日期"),
    DIGEST_FAILED("S0000025", "信息处理失败"),
    FIELD_ACCESS_FAILED("S0000026", "信息处理失败"),
    OBJECT_VALIDATION_FAILED("S0000029", "信息处理失败"),
    OBJECT_CREATION_FAILED("S0000030", "信息处理失败"),
    COPY_SOURCE_MISSING("S0000031", "缺少所需信息"),
    COPY_TARGET_MISSING("S0000032", "信息处理失败"),
    ERROR_CODE_REQUIRED("S0000033", "信息处理失败"),
    ERROR_MESSAGE_REQUIRED("S0000034", "缺少提示信息"),
    OBJECT_SERIALIZATION_FAILED("S0000035", "信息处理失败"),
    REQUIRED_VALUE_MISSING("S0000036", "未找到所需信息");

    private final String code;
    private final String message;

    CommonExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
