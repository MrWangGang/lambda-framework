package org.lambda.framework.nacos.config;

import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.NacosExceptionEnum;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.rsocket.context.RSocketServerInitializedEvent;
import org.springframework.context.event.EventListener;

public class RsocketRunListener {
    private final NacosAutoServiceRegistration registration;
    private int boundPort;

    public RsocketRunListener(NacosAutoServiceRegistration registration) {
        this.registration = registration;
    }

    @EventListener
    public void serverBound(RSocketServerInitializedEvent event) {
        boundPort = event.getServer().address().getPort();
    }

    @EventListener
    public void applicationReady(ApplicationReadyEvent event) {
        if (boundPort > 0) {
            registration.setPort(boundPort);
            try {
                registration.start();
            } catch (RuntimeException error) {
                var failure = new EventException(NacosExceptionEnum.REGISTRATION_FAILED);
                failure.initCause(error);
                throw failure;
            }
        }
    }
}
