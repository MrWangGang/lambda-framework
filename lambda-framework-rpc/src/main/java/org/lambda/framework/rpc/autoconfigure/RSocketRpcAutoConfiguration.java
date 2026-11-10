package org.lambda.framework.rpc.autoconfigure;

import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.lambda.framework.rpc.proxy.RsocketRpcProxyBeanFactoryPostProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class RSocketRpcAutoConfiguration {
    @Bean
    static RsocketRpcProxyBeanFactoryPostProcessor rsocketRpcProxyBeanFactoryPostProcessor(
            ObjectProvider<RSocketLoadbalance> loadbalance, ObjectProvider<PrincipalStash> principalStash) {
        return new RsocketRpcProxyBeanFactoryPostProcessor(loadbalance, principalStash);
    }
}
