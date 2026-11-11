package org.lambda.framework.nacos.config;

import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.GatewayRouteContract;
import org.lambda.framework.enums.NacosExceptionEnum;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.rsocket.context.RSocketServerInitializedEvent;
import org.springframework.context.event.EventListener;

public class RsocketRunListener {
    private final NacosAutoServiceRegistration registration;
    private final NacosRegistration instance;
    private final ObjectProvider<GatewayRouteContract> routes;
    private int boundPort;

    public RsocketRunListener(NacosAutoServiceRegistration registration, NacosRegistration instance,
                             ObjectProvider<GatewayRouteContract> routes) {
        this.registration = registration;
        this.instance = instance;
        this.routes = routes;
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
                routes.ifAvailable(contract ->
                        instance.getMetadata().put(GatewayRouteContract.METADATA_KEY, contract.version()));
                registration.start();
            } catch (RuntimeException error) {
                throw new EventException(NacosExceptionEnum.REGISTRATION_FAILED, null, error);
            }
        }
    }
}
