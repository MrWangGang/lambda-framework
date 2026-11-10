package org.lambda.framework.common.autoconfigure;

import org.lambda.framework.common.jackson.ObjectIdJsonModules;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ObjectIdJsonAutoConfiguration {
    @Bean
    com.fasterxml.jackson.databind.Module lambdaObjectIdJackson2Module() {
        return ObjectIdJsonModules.jackson2();
    }

    @Bean
    tools.jackson.databind.JacksonModule lambdaObjectIdJackson3Module() {
        return ObjectIdJsonModules.jackson3();
    }
}
