package org.lambda.framework.openai.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum OpaiExceptionEnum implements ExceptionEnumFunction {

    //OPEN AI组件相关   300-399
    ES_OPAI_000("ES_OPAI_000","不能发送空消息"),
    ES_OPAI_001("ES_OPAI_001","uniqueId不能为空"),
    ES_OPAI_002("ES_OPAI_002","缺少模型参数"),
    ES_OPAI_003("ES_OPAI_003","uniqueId无效"),
    ES_OPAI_004("ES_OPAI_004","openAiApiKey不能为空"),
    ES_OPAI_005("ES_OPAI_005","userId不能为空"),
    ES_OPAI_006("ES_OPAI_006","Ai-Api错误"),
    ES_OPAI_007("ES_OPAI_007","加载聊天历史错误"),
    ES_OPAI_008("ES_OPAI_008","当前会话与uniqueId不匹配"),
    ES_OPAI_009("ES_OPAI_009","必须设置responseFormat"),
    ES_OPAI_010("ES_OPAI_010","image size不在 256x256 512x512 1024x1024 的选项内"),
    ES_OPAI_011("ES_OPAI_011","必须设置image size"),
    ES_OPAI_012("ES_OPAI_012","必须设置maxTokens"),
    ES_OPAI_013("ES_OPAI_013","必须设置返回的响应数量"),
    ES_OPAI_014("ES_OPAI_014","无效配额"),
    ES_OPAI_015("ES_OPAI_015","必须设置接口调用超时时间"),
    ES_OPAI_016("ES_OPAI_016","当前配额少于此次会话所需的token"),
    ES_OPAI_017("ES_OPAI_017","当前会话超过了该服务的最大配额"),
    ES_OPAI_018("ES_OPAI_018","当前会话超过了该服务的最大配额,可以选择重新开始新一轮对话"),
    ES_OPAI_019("ES_OPAI_019","不支持的模型");
    // 成员变量
    private String code;

    private String message;
    // 构造方法
    private OpaiExceptionEnum(String code, String message) {
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
