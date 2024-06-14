package org.lambda.framework.gateway.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayGlobalJacksonConfig {
    @Bean
    public ObjectMapper globalObjectMapper() {
        return JsonUtil.getJsonFactory();
    }
}
