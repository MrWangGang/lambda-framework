package org.lambda.framework.guid;


import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.startup.DeploymentMode;
import org.lambda.framework.enums.GuidExceptionEnum;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@EnableConfigurationProperties(GuidAutoConfiguration.Settings.class)
public class GuidAutoConfiguration {
    @ConfigurationProperties("lambda.guid")
    record Settings(@DefaultValue("single") DeploymentMode mode, Integer worker) {}

    @Bean
    @ConditionalOnMissingBean
    GuidFactory guidFactory(Settings settings, Environment environment) {
        Integer worker = settings.worker();
        if (worker == null && settings.mode() == DeploymentMode.CLUSTER) {
            worker = environment.getProperty("LAMBDA_INSTANCE_INDEX", Integer.class);
            if (worker == null) {
                throw new EventException(GuidExceptionEnum.CONFIGURATION_INVALID, "Cluster Snowflake requires lambda.guid.worker or LAMBDA_INSTANCE_INDEX");
            }
        }
        return new GuidFactory(worker == null ? 0 : worker);
    }
}
