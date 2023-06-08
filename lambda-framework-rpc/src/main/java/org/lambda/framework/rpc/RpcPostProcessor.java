package org.lambda.framework.rpc;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.EventException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

import static org.lambda.framework.rpc.enums.RpcExceptionEnum.ES_RPC_000;

@Component
public class RpcPostProcessor implements ResourceLoaderAware,BeanFactoryPostProcessor  {
    private ResourceLoader resourceLoader;
    @Resource
    private ReactorLoadBalancerExchangeFilterFunction reactorLoadBalancerExchangeFilterFunction;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        try {
            //获取指定目录下的class文件
            org.springframework.core.io.Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources("classpath*:/**/facade/**/*.class");
            //根据resources创建数据读取工厂
            MetadataReaderFactory metaReader = new CachingMetadataReaderFactory(resourceLoader);
            for (org.springframework.core.io.Resource resource : resources) {
                //获取元数据
                MetadataReader metadataReader = metaReader.getMetadataReader(resource);
                //判断是否存在HttpExchange注解(是否为http interface的接口调用)
                if (metadataReader.getAnnotationMetadata().hasAnnotation(HttpExchange.class.getName())) {
                    //构建一个web客户端
                    WebClient webClient = WebClient.builder().build();
                    //根据web客户端去构建服http服务的代理工厂
                    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient)).build();
                    //利用类的全限定名通过Class.forName获取class对象并利用http服务的代理工厂创建出代理对象
                    Object client = factory.createClient(Class.forName(metadataReader.getClassMetadata().getClassName()));
                    //将创建出来的代理对象放到io容器当中
                    beanFactory.registerSingleton(metadataReader.getClassMetadata().getClassName(), client);
                }
            }
        } catch (Exception e) {
            throw new EventException(ES_RPC_000);
        }
    }
}

