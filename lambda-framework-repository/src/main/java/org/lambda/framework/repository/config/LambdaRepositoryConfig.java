package org.lambda.framework.repository.config;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.repository.config.elasticsearch.AbstractReactiveElasticsearchRepositoryConfig;
import org.lambda.framework.repository.config.mongodb.AbstractReactiveMongoRepositoryConfig;
import org.lambda.framework.repository.config.mysql.AbstractReactiveMySqlRepositoryConfig;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.ES_REPOSITORY_108;

public class LambdaRepositoryConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Class main = application.getMainApplicationClass();
        String packages = main.getPackageName();
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        // 添加需要匹配的过滤器
        scanner.addIncludeFilter(new AnnotationTypeFilter(Configuration.class));
        Set<BeanDefinition> beanDefinitions = new HashSet<>();
        beanDefinitions.addAll(scanner.findCandidateComponents(packages));
        // 获取 PropertySources
        MutablePropertySources propertySources = environment.getPropertySources();
        // 创建新的 PropertySource
        Properties properties = new Properties();

        properties.setProperty("spring.data.elasticsearch.repositories.enabled", "false");
        properties.setProperty("spring.data.redis.repositories.enabled", "false");
        properties.setProperty("spring.data.r2dbc.repositories.enabled", "false");
        properties.setProperty("spring.data.mongodb.repositories.type", "none");
        //redis使用的ReactiveRedisTemplate来进行操作的,禁用他的存储库功能
        for (BeanDefinition beanDefinition : beanDefinitions) {
            try {
                String beanClassName = beanDefinition.getBeanClassName();
                if (StringUtils.isNotBlank(beanClassName)) {
                    Class<?> beanClass = Class.forName(beanClassName);

                    if(AbstractReactiveElasticsearchRepositoryConfig.class.isAssignableFrom(beanClass)){
                        properties.setProperty("spring.data.elasticsearch.repositories.enabled", "true");
                    }
                    if(AbstractReactiveMongoRepositoryConfig.class.isAssignableFrom(beanClass)){
                        properties.setProperty("spring.data.mongodb.repositories.type", "auto");
                    }
                    if(AbstractReactiveMySqlRepositoryConfig.class.isAssignableFrom(beanClass)){
                        properties.setProperty("spring.data.r2dbc.repositories.enabled", "true");
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new EventException(ES_REPOSITORY_108);
            }
        }

        // 自定义要排除的自动配置类
        String[] excludedAutoConfigurations = {
                "org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration",
                "org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration",
        };
        properties.setProperty("spring.autoconfigure.exclude", String.join(",", excludedAutoConfigurations));
        PropertiesPropertySource propertySource = new PropertiesPropertySource("lambdaRepositoryProperties", properties);
        // 将新的 PropertySource 添加到环境中
        propertySources.addFirst(propertySource);
    }
}
