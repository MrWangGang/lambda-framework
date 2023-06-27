package org.lambda.framework.rpc;

import org.lambda.framework.common.exception.EventException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ResourceLoaderAware;
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

import static org.lambda.framework.rpc.enums.RpcExceptionEnum.ES_RPC_000;

@Component
public class RpcPostProcessor implements ResourceLoaderAware,BeanFactoryPostProcessor  {
    private ResourceLoader resourceLoader;
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
                    String className = metadataReader.getClassMetadata().getClassName();
                    beanFactory.registerSingleton(className, client);
                }
            }
        } catch (Exception e) {
            throw new EventException(ES_RPC_000);
        }
    }
}

