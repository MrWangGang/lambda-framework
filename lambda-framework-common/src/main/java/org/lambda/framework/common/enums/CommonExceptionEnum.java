package org.lambda.framework.common.enums;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum CommonExceptionEnum implements ExceptionEnumFunction {
    ES_COMMON_000("S0000000", "系统繁忙，请稍后重试"),
    ES_COMMON_001("S0000001", "缺少请求信息"),
    ES_COMMON_002("S0000002", "无法访问所需信息"),
    ES_COMMON_003("S0000003", "读取信息失败"),
    ES_COMMON_004("S0000004", "信息处理失败"),
    ES_COMMON_005("S0000005", "信息处理失败"),
    ES_COMMON_006("S0000006", "信息处理失败"),
    ES_COMMON_007("S0000007", "信息处理失败"),
    ES_COMMON_008("S0000008", "信息处理失败"),
    ES_COMMON_009("S0000009", "信息处理失败"),
    ES_COMMON_010("S0000010", "信息处理失败"),
    ES_COMMON_011("S0000011", "信息处理失败"),
    ES_COMMON_012("S0000012", "信息处理失败"),
    ES_COMMON_013("S0000013", "信息处理失败"),
    ES_COMMON_014("S0000014", "信息处理失败"),
    ES_COMMON_015("S0000015", "信息处理失败"),
    ES_COMMON_016("S0000016", "信息处理失败"),
    ES_COMMON_017("S0000017", "信息处理失败"),
    ES_COMMON_018("S0000018", "请求内容格式不正确"),
    ES_COMMON_019("S0000019", "结果处理失败"),
    ES_COMMON_020("S0000020", "处理失败，请稍后重试"),
    ES_COMMON_021("S0000021", "信息处理失败"),
    ES_COMMON_022("S0000022", "信息格式不正确"),
    ES_COMMON_023("S0000023", "请填写日期"),
    ES_COMMON_024("S0000024", "日期格式不正确"),
    ES_COMMON_025("S0000025", "信息处理失败"),
    ES_COMMON_026("S0000026", "信息处理失败"),
    ES_COMMON_027("S0000027", "暂时无法识别身份"),
    ES_COMMON_028("S0000028", "信息处理失败"),
    ES_COMMON_029("S0000029", "信息处理失败"),
    ES_COMMON_030("S0000030", "信息处理失败"),
    ES_COMMON_031("S0000031", "缺少所需信息"),
    ES_COMMON_032("S0000032", "信息处理失败"),
    ES_COMMON_033("S0000033", "信息处理失败"),
    ES_COMMON_034("S0000034", "缺少提示信息"),
    ES_COMMON_035("S0000035", "信息处理失败"),
    ES_COMMON_036("S0000036", "未找到所需信息");

    private final String code;
    private final String message;

    CommonExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
