package org.lambda.framework.compliance.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum ComplianceExceptionEnum implements ExceptionEnumFunction {

    //OPEN AI组件相关   300-399
    ES_COMPLIANCE_000("ES_COMPLIANCE_000","目标对象必须存在"),
    ES_COMPLIANCE_001("ES_COMPLIANCE_001","创建节点时,节点信息必须存在"),
    ES_COMPLIANCE_002("ES_COMPLIANCE_002","创建节点时，不能指定对象id"),
    ES_COMPLIANCE_003("ES_COMPLIANCE_003","此机构已经创建了树模型，无法创建根"),
    ES_COMPLIANCE_004("ES_COMPLIANCE_004","递归->达到最大深度"),
    ES_COMPLIANCE_005("ES_COMPLIANCE_005","递归->出现循环引用"),
    ES_COMPLIANCE_006("ES_COMPLIANCE_006","当前节点不存在"),
    ES_COMPLIANCE_007("ES_COMPLIANCE_007","目标节点不存在"),
    ES_COMPLIANCE_008("ES_COMPLIANCE_008","不能移动根节点"),
    ES_COMPLIANCE_009("ES_COMPLIANCE_009","重复的根节点"),
    ES_COMPLIANCE_010("ES_COMPLIANCE_010","目标节点传入值不符合规范 targetNodeId < -1"),
    ES_COMPLIANCE_011("ES_COMPLIANCE_011","当前节点不能为null"),
    ES_COMPLIANCE_012("ES_COMPLIANCE_012","无效的数据操作,没有操作者对象"),
    ES_COMPLIANCE_013("ES_COMPLIANCE_013","节点所属机构ID不能为空"),
    ES_COMPLIANCE_014("ES_COMPLIANCE_014","实例化tree对象失败"),
    ES_COMPLIANCE_015("ES_COMPLIANCE_015","修改节点时，目标id必须有效"),
    ES_COMPLIANCE_016("ES_COMPLIANCE_016","根节点不允许删除");




    private String code;

    private String message;
    // 构造方法
    private ComplianceExceptionEnum(String code, String message) {
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
