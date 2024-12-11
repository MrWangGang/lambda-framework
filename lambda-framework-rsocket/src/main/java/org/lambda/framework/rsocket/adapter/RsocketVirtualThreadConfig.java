package org.lambda.framework.rsocket.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // 设置较高的优先级顺序，可根据实际情况调整数值
public class RsocketVirtualThreadConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> customProperties = new HashMap<>();
        customProperties.put("spring.main.web-application-type", "none");
        customProperties.put("spring.threads.virtual.enabled", "true");
        environment.getPropertySources().addFirst(new MapPropertySource("rsocketVirtualThreadConfigProperties", customProperties));

    }
}
