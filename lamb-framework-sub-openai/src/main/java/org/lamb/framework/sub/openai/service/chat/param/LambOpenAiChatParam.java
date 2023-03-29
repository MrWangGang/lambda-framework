package org.lamb.framework.sub.openai.service.chat.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.lamb.framework.sub.openai.LambOpenAiParam;
import org.lamb.framework.sub.openai.LambOpenAiUniqueParam;

@Getter
@Setter
public class LambOpenAiChatParam extends LambOpenAiParam {

    //AI人设
    private String persona;

    //使用什么采样温度，介于 0 和 2 之间。较高的值（如 0.8）将使输出更加随机，
    // 而较低的值（如 0.2）将使其更加集中和确定。
    // 我们通常建议更改此或top_p但不能同时更改两者。
    private Double temperature;

    //使用温度采样的替代方法称为核心采样，
    // 其中模型考虑具有top_p概率质量的令牌的结果。因此，0.1 意味着只考虑包含前 10% 概率质量的代币。
    // 我们通常建议改变这个或温度，但不要两者兼而有之。
    private Double topP;

    //如果设置，将发送部分消息增量，就像在 ChatGPT 中一样。
    // 令牌将在可用时作为纯数据服务器发送的事件发送，流由 data： [DONE] 消息终止。
    // 有关示例代码，请参阅 OpenAI 说明书
    private Boolean stream;

    //要在聊天完成中生成的最大令牌数。 输入令牌和生成的令牌的总长度受模型上下文长度的限制。

    //介于 -2.0 和 2.0 之间的数字。正值会根据新标记到目前为止是否出现在文本中来惩罚它们，
    // 从而增加模型讨论新主题的可能性。 查看有关频率和状态处罚的更多信息
    private Double presencePenalty;

    //介于 -2.0 和 2.0 之间的数字。
    // 正值会根据新标记到目前为止在文本中的现有频率来惩罚新标记，从而降低模型逐字重复同一行的可能性。
    private Double frequencyPenalty;

    @Builder
    public LambOpenAiChatParam(Integer n, String prompt, Long quota, Integer maxTokens, Long timeOut, String openAiApiKey, String userId, LambOpenAiUniqueParam lambOpenAiUniqueParam, String persona, Double temperature, Double topP, Boolean stream, Double presencePenalty, Double frequencyPenalty) {
        super(n, prompt, quota, maxTokens, timeOut, openAiApiKey, userId, lambOpenAiUniqueParam);
        this.persona = persona;
        this.temperature = temperature;
        this.topP = topP;
        this.stream = stream;
        this.presencePenalty = presencePenalty;
        this.frequencyPenalty = frequencyPenalty;
    }
}
