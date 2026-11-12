package org.lambda.framework.nacos.autoconfigure;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.GatewayRouteContract;
import org.lambda.framework.common.support.ServiceInstanceSource;
import org.lambda.framework.common.support.ServiceInstanceSource.Endpoint;
import org.lambda.framework.enums.NacosExceptionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@AutoConfiguration(after = NacosDiscoveryAutoConfiguration.class)
@ConditionalOnBean({NacosServiceDiscovery.class, NacosServiceManager.class})
public class NacosInstanceSourceAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(NacosInstanceSourceAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(ServiceInstanceSource.class)
    ServiceInstanceSource nacosInstanceSource(NacosServiceManager manager, NacosDiscoveryProperties properties) {
        var scheduler = Schedulers.boundedElastic();
        return service -> Flux.defer(() -> {
                    var naming = manager.getNamingService(properties.getNacosProperties());
                    String group = properties.getGroup();
                    return changes(naming, service, group)
                            .publishOn(scheduler, 1)
                            .map(ignored -> endpoints(naming, service, group));
                })
                .subscribeOn(scheduler, false)
                .cancelOn(scheduler)
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(250))
                        .maxBackoff(Duration.ofSeconds(10)).transientErrors(true));
    }

    private static Flux<Boolean> changes(NamingService naming, String service, String group) {
        return Flux.create(sink -> {
            EventListener listener = event -> {
                if (event instanceof NamingEvent) {
                    sink.next(true);
                }
            };
            try {
                naming.subscribe(service, group, listener);
            } catch (NacosException | RuntimeException error) {
                // The SDK registers the local listener before attempting the remote subscription.
                unsubscribe(naming, service, group, listener);
                sink.error(new EventException(NacosExceptionEnum.SUBSCRIPTION_FAILED, null, error));
                return;
            }
            sink.onDispose(() -> unsubscribe(naming, service, group, listener));
            sink.next(true);
        }, FluxSink.OverflowStrategy.LATEST);
    }

    private static Set<Endpoint> endpoints(NamingService naming, String service, String group) {
        try {
            // Use the SDK's current view so discovery still honors its local failover and health filtering.
            var endpoints = new HashSet<Endpoint>();
            for (var instance : naming.selectInstances(service, group, true)) {
                if (instance != null && instance.isEnabled() && instance.isHealthy()) {
                    var metadata = instance.getMetadata();
                    String version = metadata == null ? "" : metadata.getOrDefault(GatewayRouteContract.METADATA_KEY, "");
                    endpoints.add(new Endpoint(instance.getIp(), instance.getPort(), version));
                }
            }
            return Collections.unmodifiableSet(endpoints);
        } catch (NacosException error) {
            throw new EventException(NacosExceptionEnum.SUBSCRIPTION_FAILED, null, error);
        }
    }

    private static void unsubscribe(NamingService naming, String service, String group, EventListener listener) {
        try {
            naming.unsubscribe(service, group, listener);
        } catch (NacosException | RuntimeException error) {
            log.debug("Could not unsubscribe from Nacos service {}", service, error);
        }
    }
}
