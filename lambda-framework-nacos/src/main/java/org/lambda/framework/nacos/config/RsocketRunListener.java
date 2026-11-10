package org.lambda.framework.nacos.config;

import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import org.springframework.boot.rsocket.context.RSocketServerInitializedEvent;
import org.springframework.context.ApplicationListener;

public class RsocketRunListener implements ApplicationListener<RSocketServerInitializedEvent> {
    private final NacosAutoServiceRegistration registration;

    public RsocketRunListener(NacosAutoServiceRegistration registration) {
        this.registration = registration;
    }

    @Override
    public void onApplicationEvent(RSocketServerInitializedEvent event) {
        registration.setPort(event.getServer().address().getPort());
        registration.start();
    }
}
