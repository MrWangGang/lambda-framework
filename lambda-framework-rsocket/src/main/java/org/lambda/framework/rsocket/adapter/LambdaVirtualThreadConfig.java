package org.lambda.framework.rsocket.adapter;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;
//在rsocket中 虚拟线程还没有得到应用，
// 原因是因为netty还没有支持虚拟线程，
// 这个配置开启了也是没有用
//期待有一天netty可以拥抱虚拟线程
public class LambdaVirtualThreadConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        // 自定义要排除的自动配置类
        // 获取 PropertySources
        MutablePropertySources propertySources = environment.getPropertySources();
        // 创建新的 PropertySource
        Properties properties = new Properties();
        properties.setProperty("spring.threads.virtual.enabled","true");
        PropertiesPropertySource propertySource = new PropertiesPropertySource("lambdaWebProperties", properties);
        // 将新的 PropertySource 添加到环境中
        propertySources.addFirst(propertySource);
    }
}
