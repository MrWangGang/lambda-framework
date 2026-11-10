package org.lambda.framework.common.startup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@AutoConfiguration
@ConditionalOnBooleanProperty(name = "lambda.startup.check", matchIfMissing = true)
public class ComponentChecksAutoConfiguration {
    @Bean
    ApplicationRunner lambdaComponentChecks(List<ComponentCheck> checks,
            @Value("${lambda.startup.timeout:8s}") Duration timeout) {
        return args -> Flux.fromIterable(checks)
                .flatMap(check -> check.run(timeout))
                .then()
                .block();
    }
}
