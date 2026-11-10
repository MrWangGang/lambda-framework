package org.lambda.framework.nacos.autoconfigure;

import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistryAutoConfiguration;
import org.lambda.framework.nacos.config.RsocketRunListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = NacosServiceRegistryAutoConfiguration.class)
@ConditionalOnProperty("spring.rsocket.server.port")
@ConditionalOnBean(NacosAutoServiceRegistration.class)
public class RSocketNacosAutoConfiguration {
    @Bean
    RsocketRunListener rsocketRunListener(NacosAutoServiceRegistration registration) {
        return new RsocketRunListener(registration);
    }
}
