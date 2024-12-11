package org.lambda.framework.rsocket.adapter;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;
public class RsocketVirtualThreadConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        // 自定义要排除的自动配置类
        // 获取 PropertySources
        MutablePropertySources propertySources = environment.getPropertySources();
        // 创建新的 PropertySource
        Properties properties = new Properties();
        properties.setProperty("spring.threads.virtual.enabled","true");
        properties.setProperty("spring.main.web-application-type","none");
        PropertiesPropertySource propertySource = new PropertiesPropertySource("lambdaRsocketProperties", properties);
        // 将新的 PropertySource 添加到环境中
        propertySources.addFirst(propertySource);
    }
}
