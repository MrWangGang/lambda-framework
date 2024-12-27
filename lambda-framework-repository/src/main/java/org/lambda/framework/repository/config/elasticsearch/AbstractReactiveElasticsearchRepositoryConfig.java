package org.lambda.framework.repository.config.elasticsearch;

import org.lambda.framework.common.enums.ConverterEnum;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.repository.config.converter.EnumReadConverter;
import org.lambda.framework.repository.config.converter.EnumWriteConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

import java.time.Duration;
import java.util.Arrays;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public abstract class AbstractReactiveElasticsearchRepositoryConfig extends ReactiveElasticsearchConfiguration {
    // 配置抽象方法供子类实现
    protected abstract String host(); // 主机地址（支持多个）
    protected abstract String user(); // 用户名
    protected abstract String password(); // 密码
    protected abstract Integer connectTimeoutSeconds(); // 连接超时时间
    protected abstract Integer socketTimeoutSeconds(); // 读写超时时间

    @Override
    public ClientConfiguration clientConfiguration() {
        Assert.verify(this.host(),ES_REPOSITORY_ELASTICSEARCH_020);
        Assert.verify(this.user(),ES_REPOSITORY_ELASTICSEARCH_021);
        Assert.verify(this.password(),ES_REPOSITORY_ELASTICSEARCH_022);
        Assert.verify(this.connectTimeoutSeconds(),ES_REPOSITORY_ELASTICSEARCH_023);
        Assert.verify(this.socketTimeoutSeconds(),ES_REPOSITORY_ELASTICSEARCH_024);

        String[] hosts = this.host().split(",");
        return ClientConfiguration.builder()
                .connectedTo(hosts)
                .withBasicAuth(this.user(),this.password())
                .withConnectTimeout(Duration.ofSeconds(this.connectTimeoutSeconds()))
                .withSocketTimeout(Duration.ofSeconds(this.socketTimeoutSeconds()))
                .build();
    }

    @Bean
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(Arrays.asList(new EnumReadConverter<ConverterEnum>(),new EnumWriteConverter())); // 注册你的自定义转换器
    }
}
