package org.lambda.framework.openai.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum OpenaiExceptionEnum implements ExceptionEnumFunction {

    //OPEN AI组件相关   300-399
    ES_OPENAI_000("ES_OPENAI_000","不能发送空消息"),
    ES_OPENAI_001("ES_OPENAI_001","uniqueId不能为空"),
    ES_OPENAI_002("ES_OPENAI_002","缺少模型参数"),
    ES_OPENAI_003("ES_OPENAI_003","uniqueId无效"),
    ES_OPENAI_004("ES_OPENAI_004","openAiApiKey不能为空"),
    ES_OPENAI_005("ES_OPENAI_005","userId不能为空"),
    ES_OPENAI_006("ES_OPENAI_006","Ai-Api错误"),
    ES_OPENAI_007("ES_OPENAI_007","加载聊天历史错误"),
    ES_OPENAI_008("ES_OPENAI_008","当前会话与uniqueId不匹配"),
    ES_OPENAI_009("ES_OPENAI_009","必须设置responseFormat"),
    ES_OPENAI_010("ES_OPENAI_010","image size不在 256x256 512x512 1024x1024 的选项内"),
    ES_OPENAI_011("ES_OPENAI_011","必须设置image size"),
    ES_OPENAI_012("ES_OPENAI_012","必须设置maxTokens"),
    ES_OPENAI_013("ES_OPENAI_013","必须设置返回的响应数量"),
    ES_OPENAI_014("ES_OPENAI_014","无效配额"),
    ES_OPENAI_015("ES_OPENAI_015","必须设置接口调用超时时间"),
    ES_OPENAI_016("ES_OPENAI_016","当前配额少于此次会话所需的token"),
    ES_OPENAI_017("ES_OPENAI_017","当前会话超过了该服务的最大配额"),
    ES_OPENAI_018("ES_OPENAI_018","当前会话超过了该服务的最大配额,可以选择重新开始新一轮对话"),
    ES_OPENAI_019("ES_OPENAI_019","不支持的模型");
    // 成员变量
    private String code;

    private String message;
    // 构造方法
    private OpenaiExceptionEnum(String code, String message) {
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
