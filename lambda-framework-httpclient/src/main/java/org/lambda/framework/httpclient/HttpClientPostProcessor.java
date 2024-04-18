package org.lambda.framework.httpclient;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.EventException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.lambda.framework.httpclient.enums.HttpClientExceptionEnum.ES_RPC_000;

@Component
public class HttpClientPostProcessor implements ResourceLoaderAware, SmartInitializingSingleton, ApplicationContextAware {
    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    @Resource
    private ConfigurableListableBeanFactory beanFactory;

    @Resource
    private ReactorLoadBalancerExchangeFilterFunction reactorLoadBalancerExchangeFilterFunction;
    private ApplicationContext applicationContext;
    @Override
    public void afterSingletonsInstantiated() {
        try {
            //获取指定目录下的class文件
            org.springframework.core.io.Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources("classpath:/**/httpclient/**/*.class");
            //根据resources创建数据读取工厂
            MetadataReaderFactory metaReader = new CachingMetadataReaderFactory(resourceLoader);
            for (org.springframework.core.io.Resource resource : resources) {
                //获取元数据
                MetadataReader metadataReader = metaReader.getMetadataReader(resource);
                //判断是否存在HttpExchange注解(是否为http interface的接口调用)
                if (metadataReader.getAnnotationMetadata().hasAnnotation(HttpClient.class.getName())) {
                    //构建一个web客户端
                    Map rpcAttributes = metadataReader.getAnnotationMetadata().getAnnotationAttributes(HttpClient.class.getName());
                    WebClient webClient = WebClient.builder().build();
                    if(rpcAttributes!=null){
                        List<ExchangeFilterFunction> customerConsumer = null;
                        List<ExchangeFilterFunction> loadnBlanceConsumer = null;
                        List<ExchangeFilterFunction> allConsumer = new ArrayList<>();
                       if(rpcAttributes.get("filter") !=null){
                           Class<ExchangeFilterFunction>[] filterClasses =  (Class<ExchangeFilterFunction>[])rpcAttributes.get("filter");
                           if(filterClasses!=null){
                                if(filterClasses.length>0){
                                    customerConsumer = Arrays.stream(filterClasses).toList().stream()
                                            .map(clazz -> {
                                             return applicationContext.getBean(clazz);
                                            })
                                            .collect(Collectors.toList());
                                }
                            }
                       }
                        if (rpcAttributes.get("balance") != null) {
                            boolean blance = (Boolean) rpcAttributes.get("balance");
                            if(blance){
                                List<ExchangeFilterFunction> list = new ArrayList<ExchangeFilterFunction>();
                                list.add(reactorLoadBalancerExchangeFilterFunction);
                                loadnBlanceConsumer = list;
                            }
                        }
                        if(customerConsumer!=null){
                            allConsumer.addAll(customerConsumer);
                        }
                        if(loadnBlanceConsumer!=null){
                            allConsumer.addAll(loadnBlanceConsumer);
                        }
                        if(allConsumer!=null)
                            if(allConsumer.size()>0){
                                if(allConsumer.get(0)!=null){
                                    Consumer<List<ExchangeFilterFunction>> filterConsumer = exchangeFilterFunctions -> {
                                        exchangeFilterFunctions.addAll(allConsumer);
                                    };
                                    webClient = WebClient.builder().filters(filterConsumer).build();
                                }
                            }
                    }
                    //根据web客户端去构建服http服务的代理工厂
                    HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
                    //利用类的全限定名通过Class.forName获取class对象并利用http服务的代理工厂创建出代理对象
                    Object client = factory.createClient(Class.forName(metadataReader.getClassMetadata().getClassName()));
                    //将创建出来的代理对象放到spring容器当中
                    String className = metadataReader.getClassMetadata().getClassName();
                    beanFactory.registerSingleton(className, client);
                }
            }
        } catch (Exception e) {
            throw new EventException(ES_RPC_000);
        }

    }

}
