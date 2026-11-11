package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum RepositoryExceptionEnum implements ExceptionEnumFunction {
    CHANGE_STREAM_CONFIGURATION_INVALID("S0510016", "数据暂不可用"),
    CHANGE_STREAM_ID_TYPE_UNSUPPORTED("S0510017", "数据暂不可用"),
    COLLECTION_MAPPING_REQUIRED("S0510018", "数据暂不可用"),
    COLLECTION_NAME_REQUIRED("S0510019", "数据暂不可用"),
    INVALID_PAGING("S0500100", "分页参数不正确"),
    PAGING_REQUIRED("S0500103", "缺少分页参数"),
    UNKNOWN_ENUM_VALUE("S0500106", "无法识别数据状态"),
    UNKNOWN_CHANGE_OPERATION("S0500107", "无法读取数据变更"),
    CONFIGURATION_INVALID("S0500108", "数据组件配置不正确");

    private final String code;
    private final String message;

    RepositoryExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
