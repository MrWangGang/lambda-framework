package org.lambda.framework.nacos.config;

import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.EventException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.rsocket.context.RSocketServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import static org.lambda.framework.nacos.enums.NacosExceptionEnum.ES_NACOS_000;

@Component
public class RsocketRunListener implements ApplicationListener<RSocketServerInitializedEvent> {
    @Resource
    private NacosAutoServiceRegistration registration;
    @Value("${spring.rsocket.server.port:-1}")
    Integer port;
    @Override
    public void onApplicationEvent(RSocketServerInitializedEvent event) {
        if(port == -1L || port == null)throw new EventException(ES_NACOS_000);
        if (registration != null && port != null) {
            registration.setPort(port);
            registration.start();
            return;
        }
        throw new EventException(ES_NACOS_000);
    }
}
