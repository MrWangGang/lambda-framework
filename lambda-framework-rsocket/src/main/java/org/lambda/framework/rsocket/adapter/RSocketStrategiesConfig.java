package org.lambda.framework.rsocket.adapter;

import org.lambda.framework.common.util.sample.JsonUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;

@Configuration
public class RSocketStrategiesConfig {
    @Bean
    public RSocketStrategies rSocketStrategies() {
        return org.springframework.messaging.rsocket.RSocketStrategies.builder()
                .decoder(new Jackson2JsonDecoder(JsonUtil.getJsonFactory()))
                .encoder(new Jackson2JsonEncoder(JsonUtil.getJsonFactory()))
                .build();
    }
}
