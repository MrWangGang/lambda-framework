package org.lambda.framework.sub.openai;

public enum OpenAiModelEnum {

    TURBO("gpt-3.5-turbo");

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    private String model;

    private OpenAiModelEnum(String model){
        this.model = model;
    }

}
