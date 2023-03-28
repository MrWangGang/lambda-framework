package org.lamb.framework.sub.openai;

public enum LambOpenAiModelEnum {

    TURBO("gpt-3.5-turbo");

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    private String model;

    private LambOpenAiModelEnum(String model){
        this.model = model;
    }

}
