package org.lambda.framework.nacos.autoconfigure;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import org.lambda.framework.common.support.GatewayRouteContract;
import org.lambda.framework.common.support.ServiceInstanceSource;
import org.lambda.framework.common.support.ServiceInstanceSource.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.stream.Collectors;

@AutoConfiguration(after = NacosDiscoveryAutoConfiguration.class)
@ConditionalOnBean({NacosServiceDiscovery.class, NacosServiceManager.class})
public class NacosInstanceSourceAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(NacosInstanceSourceAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(ServiceInstanceSource.class)
    ServiceInstanceSource nacosInstanceSource(NacosServiceManager manager, NacosDiscoveryProperties properties,
                                              NacosServiceDiscovery discovery) {
        return service -> Flux.<Boolean>create(sink -> {
                    var naming = manager.getNamingService(properties.getNacosProperties());
                    String group = properties.getGroup();
                    EventListener listener = event -> {
                        if (event instanceof NamingEvent) {
                            sink.next(true);
                        }
                    };
                    try {
                        naming.subscribe(service, group, listener);
                    } catch (NacosException error) {
                        sink.error(error);
                        return;
                    }
                    sink.onDispose(() -> {
                        try {
                            naming.unsubscribe(service, group, listener);
                        } catch (NacosException error) {
                            log.debug("Could not unsubscribe from Nacos service {}", service, error);
                        }
                    });
                    sink.next(true);
                }, FluxSink.OverflowStrategy.LATEST)
                .subscribeOn(Schedulers.boundedElastic())
                .cancelOn(Schedulers.boundedElastic())
                .concatMap(ignored -> Mono.fromCallable(() -> discovery.getInstances(service))
                        .subscribeOn(Schedulers.boundedElastic()), 0)
                .map(instances -> instances.stream()
                        .map(instance -> new Endpoint(instance.getHost(), instance.getPort(),
                                instance.getMetadata().getOrDefault(GatewayRouteContract.METADATA_KEY, "")))
                        .collect(Collectors.toUnmodifiableSet()))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(250))
                        .maxBackoff(Duration.ofSeconds(10)).transientErrors(true));
    }
}
