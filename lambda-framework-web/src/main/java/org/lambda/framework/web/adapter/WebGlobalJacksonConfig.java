package org.lambda.framework.web.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebGlobalJacksonConfig {
    @Bean
    public ObjectMapper objectMapper(ObjectMapper globalObjectMapper) {
        return globalObjectMapper.copy();
    }

    @Bean
    public ObjectMapper globalObjectMapper() {
        return JsonUtil.getJsonFactory();
    }
}
