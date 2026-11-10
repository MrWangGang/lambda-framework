package org.lambda.framework.repository.enums;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum RepositoryExceptionEnum implements ExceptionEnumFunction {
    ES_REPOSITORY_MONGO_004("S0510004", "数据暂不可用"),
    ES_REPOSITORY_MONGO_005("S0510005", "数据暂不可用"),
    ES_REPOSITORY_MONGO_006("S0510006", "数据暂不可用"),
    ES_REPOSITORY_MONGO_007("S0510007", "数据暂不可用"),
    ES_REPOSITORY_MONGO_008("S0510008", "数据暂不可用"),
    ES_REPOSITORY_MONGO_009("S0510009", "数据暂不可用"),
    ES_REPOSITORY_MONGO_010("S0510010", "数据暂不可用"),
    ES_REPOSITORY_MONGO_011("S0510011", "数据暂不可用"),
    ES_REPOSITORY_MONGO_012("S0510012", "数据暂不可用"),
    ES_REPOSITORY_MONGO_013("S0510013", "数据暂不可用"),
    ES_REPOSITORY_MONGO_014("S0510014", "数据暂不可用"),
    ES_REPOSITORY_MONGO_015("S0510015", "数据暂不可用"),
    ES_REPOSITORY_MONGO_016("S0510016", "数据暂不可用"),
    ES_REPOSITORY_MONGO_017("S0510017", "数据暂不可用"),
    ES_REPOSITORY_MONGO_018("S0510018", "数据暂不可用"),
    ES_REPOSITORY_MONGO_019("S0510019", "数据暂不可用"),
    ES_REPOSITORY_REDIS_030("S0520030", "数据暂不可用"),
    ES_REPOSITORY_REDIS_031("S0520031", "数据暂不可用"),
    ES_REPOSITORY_REDIS_032("S0520032", "数据暂不可用"),
    ES_REPOSITORY_REDIS_033("S0520033", "数据暂不可用"),
    ES_REPOSITORY_REDIS_034("S0520034", "数据暂不可用"),
    ES_REPOSITORY_REDIS_035("S0520035", "数据暂不可用"),
    ES_REPOSITORY_REDIS_036("S0520036", "数据暂不可用"),
    ES_REPOSITORY_REDIS_037("S0520037", "数据暂不可用"),
    ES_REPOSITORY_REDIS_039("S0520039", "数据暂不可用"),
    ES_REPOSITORY_REDIS_040("S0520040", "数据暂不可用"),
    ES_REPOSITORY_100("S0500100", "分页参数不正确"),
    ES_REPOSITORY_101("S0500101", "数据暂不可用"),
    ES_REPOSITORY_102("S0500102", "页码必须从一开始"),
    ES_REPOSITORY_103("S0500103", "缺少分页参数"),
    ES_REPOSITORY_104("S0500104", "缺少页码"),
    ES_REPOSITORY_105("S0500105", "缺少每页条数"),
    ES_REPOSITORY_106("S0500106", "无法识别数据状态"),
    ES_REPOSITORY_107("S0500107", "无法读取数据变更"),
    ES_REPOSITORY_108("S0500108", "数据暂不可用"),
    ES_REPOSITORY_109("S0500109", "未获取到所需数据");

    private final String code;
    private final String message;

    RepositoryExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
