package org.lambda.framework.sub.openai;

public enum OpenAiModelEnum {

    TURBO("gpt-3.5-turbo",4096);

    public String getModel() {
        return model;
    }


    public void setModel(String model) {
        this.model = model;
    }

    private String model;


    public Integer getMaxToken() {
        return maxToken;
    }

    public void setMaxToken(Integer maxToken) {
        this.maxToken = maxToken;
    }

    private Integer maxToken;


    private OpenAiModelEnum(String model,Integer maxToken){
        this.maxToken = maxToken;
        this.model = model;
    }
}
