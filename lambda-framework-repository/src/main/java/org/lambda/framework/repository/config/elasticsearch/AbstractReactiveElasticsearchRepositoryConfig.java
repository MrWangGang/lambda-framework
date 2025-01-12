package org.lambda.framework.repository.config.elasticsearch;

import jakarta.annotation.Resource;
import org.apache.commons.logging.LogFactory;
import org.lambda.framework.common.enums.ConverterEnum;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.repository.config.converter.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.logging.Logger;

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
        return new ElasticsearchCustomConversions(Arrays.asList(
                new EnumReadConverter<ConverterEnum>(),
                new EnumWriteConverter(),
                new Decimal128ReadConverter(),
                new Decimal128WriteConverter(),
                new BigDecimalReadConverter(),
                new BigDecimalWriteConverter(),
                new LocalDateReadConverter(),
                new LocalDateWriteConverter(),
                new LocalDateTimeReadConverter(),
                new LocalDateTimeWriteConverter()
        ));
    }

    private static final Logger logger = Logger.getLogger(LogFactory.class.getName());


    @Resource
    @Lazy
    private ElasticsearchConverter elasticsearchConverter;

    @Resource
    @Lazy
    private ReactiveElasticsearchTemplate reactiveElasticsearchTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initIndicesAfterStartup() {
        logger.info("Elasticsearch InitIndicesAfterStartup init");
        var init = System.currentTimeMillis();
        var mappingContext = this.elasticsearchConverter.getMappingContext();
        if (mappingContext instanceof SimpleElasticsearchMappingContext) {
            SimpleElasticsearchMappingContext simpleElasticsearchMappingContext = (SimpleElasticsearchMappingContext) mappingContext;
            for (ElasticsearchPersistentEntity<?> persistentEntity : simpleElasticsearchMappingContext.getPersistentEntities()) {
                var clazz = persistentEntity.getType();
                if (clazz.isAnnotationPresent(Document.class) &&!clazz.getSuperclass().isAnnotationPresent(Document.class)) {
                    var indexOperations =reactiveElasticsearchTemplate.indexOps(clazz);  // 创建索引，如果已存在则不创建（可根据需求调整为更新等操作）
                    var mappingFromEntity = indexOperations.createMapping();
                        indexOperations.putMapping(mappingFromEntity).subscribe();
                }
            }
        }
        logger.info("Mongo InitIndicesAfterStartup take: {}"+(System.currentTimeMillis() - init));
    }
}
