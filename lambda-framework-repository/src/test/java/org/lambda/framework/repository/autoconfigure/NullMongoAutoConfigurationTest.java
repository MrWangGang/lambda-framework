package org.lambda.framework.repository.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.lambda.framework.repository.config.NullWritingMongoMappingContext;
import org.lambda.framework.repository.config.NullWritingMongoConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

class NullMongoAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MongoDataAutoConfiguration.class, MongoReactiveAutoConfiguration.class,
                    DataMongoAutoConfiguration.class, DataMongoReactiveAutoConfiguration.class))
            .withPropertyValues("lambda.repository.mongo.uri=mongodb://localhost:27017/test?replicaSet=rs0");

    @Test
    void writesNullFieldsByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(MongoMappingContext.class)).isInstanceOf(NullWritingMongoMappingContext.class);
            assertThat(context.getBean(MappingMongoConverter.class)).isInstanceOf(NullWritingMongoConverter.class);
        });
    }

    @Test
    void explicitOptOutKeepsTheDefaultMapping() {
        runner.withPropertyValues("lambda.repository.mongo.write-null-fields=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(MongoMappingContext.class)).isNotInstanceOf(NullWritingMongoMappingContext.class);
        });
    }
}
