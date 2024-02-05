package org.lambda.framework.repository.config;


import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;

@AutoConfigureBefore({R2dbcAutoConfiguration.class, R2dbcDataAutoConfiguration.class})
public class ExcludeR2dbcAutoConfig {
    //默认去掉r2dbc的自动配置，否则无法多个r2dbc存在
}
