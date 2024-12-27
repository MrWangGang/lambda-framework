package org.lambda.framework.rpc.adapter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcApi;
import org.lambda.framework.common.annotation.rsocket.RSocketRpcDiscorvery;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.rpc.adapter.support.CustomClassLoader;
import org.lambda.framework.rpc.adapter.support.EnableRSocketRpcDiscorvery;
import org.springframework.asm.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

import static org.lambda.framework.rpc.adapter.support.CamelCase.buildMessageMapping;
import static org.lambda.framework.rpc.adapter.support.CamelCase.xtoCamelCase;
import static org.lambda.framework.rpc.enums.RpcExceptionEnum.*;
import static org.springframework.asm.Opcodes.ASM7;


@Component
public class RsocketAsmProxyClassFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private static final Logger logger = Logger.getLogger(LogFactory.class.getName());

    public static CustomClassLoader customClassLoader = new CustomClassLoader();

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Class main = getMainApplicationClass(applicationContext);
        String[] basePackages = getBasePackages(main);

        // 创建一个 ClassPathScanningCandidateComponentProvider 实例
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        // 添加需要匹配的过滤器
        scanner.addIncludeFilter(new InterfaceAnnotationTypeFilter(RSocketRpcDiscorvery.class));
        // 根据操作系统类型处理类路径分隔符
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 将包路径转换为资源路径，并查找匹配的资源
        // 存储符合条件的类
        Set<BeanDefinition> beanDefinitions = new HashSet<>();
        for(String s : basePackages){
            beanDefinitions.addAll(scanner.findCandidateComponents(s));
        }
        for (BeanDefinition beanDefinition : beanDefinitions) {
            try {
                String beanClassName = beanDefinition.getBeanClassName();
                if (StringUtils.isNotBlank(beanClassName)) {
                    Class<?> beanClass = Class.forName(beanClassName);
                    if (hasInterfaceAnnotatedWithRSocket(beanClass)) {
                        Class<?> intf  = this.getAnnotatedInterfaces(beanClass, RSocketRpcDiscorvery.class);
                        String serviceName = intf.getAnnotation(RSocketRpcDiscorvery.class).value();
                        if(StringUtils.isBlank(serviceName)){
                            throw new EventException(ES_RPC_004);
                        }
                        logger.info("ASM开始执行addMessageMappingToImplementedMethods");
                        byte[] modifiedClassBytes = addMessageMappingToImplementedMethods(beanClass,serviceName);
                        logger.info("ASM开始执行addControllerAnnotation");
                        byte[] clazz = addControllerAnnotation(modifiedClassBytes);
                        logger.info("ASM开始执行customClassLoader");
                        logger.info("beanClass.getName():"+beanClass.getName());
                        Class classt = customClassLoader.loadClass(clazz);
                        logger.info("ASM结束执行customClassLoader");
                        //输出到文件方便调试
                        //Files.write(Paths.get("ModifiedClass.class"), clazz);
                        registerClassWithAnnotations(xtoCamelCase(beanClass.getSimpleName()),classt, registry);
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new EventException(ES_RPC_018);
            } catch (IOException e) {
                throw new EventException(ES_RPC_019);
            }
        }
    }

    private Class<?> getAnnotatedInterfaces(Class<?> beanClass, Class<? extends Annotation> annotationClass) {
        List<Class<?>> annotatedInterfaces = new ArrayList<>();
        for (Class<?> iface : beanClass.getInterfaces()) {
            if (iface.isAnnotationPresent(annotationClass)) {
                annotatedInterfaces.add(iface);
            }
        }
        if(!Assert.verify(annotatedInterfaces)){
            throw new EventException(ES_RPC_002);
        }
        if(annotatedInterfaces.size()>1){
            throw new EventException(ES_RPC_002);
        }
        return annotatedInterfaces.get(0);
    }

    private String[] getBasePackages(Class<?> mainApplicationClass) {
        EnableRSocketRpcDiscorvery enableRSocketRpcDiscorvery = mainApplicationClass.getAnnotation(EnableRSocketRpcDiscorvery.class);
        if (enableRSocketRpcDiscorvery != null && enableRSocketRpcDiscorvery.scanBasePackages().length > 0) {
            return enableRSocketRpcDiscorvery.scanBasePackages();
        } else {
            throw new EventException(ES_RPC_026);
        }
    }

    private Class<?> getMainApplicationClass(ApplicationContext context) {
        // 查找标注了 @BootApplication 的类
        for (String beanName : context.getBeanDefinitionNames()) {
            Class<?> beanType = context.getType(beanName);
            if (beanType != null && beanType.isAnnotationPresent(SpringBootApplication.class)) {
                return beanType;
            }
        }
        throw new EventException(ES_RPC_022);
    }

    private static boolean hasInterfaceAnnotatedWithRSocket(Class<?> clazz) {
        for (Class<?> interfaze : clazz.getInterfaces()) {
            if (interfaze.isAnnotationPresent(RSocketRpcDiscorvery.class)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] addControllerAnnotation(byte[] originalClassBytes) {
        ClassReader classReader = new ClassReader(originalClassBytes);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        classReader.accept(new ClassVisitor(ASM7, classWriter) {
            boolean foundControllerAnnotation = false;

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if(!descriptor.equals(Type.getDescriptor(RSocketRpcApi.class))){
                    //如果注解不是@RSocketApi就抛出异常
                    //只能存在一个注解，避免被spring提前注入
                    throw new EventException(ES_RPC_025);
                }
                foundControllerAnnotation = true;
                //删掉RSocketApi 注解，准备替换成@Controller
                return null;
            }

            @Override
            public void visitEnd() {
                // 如果没有找到 @RSocketRpcApi 注解，则抛出异常，强校验比不自动添加好
                if (foundControllerAnnotation) {
                    AnnotationVisitor av = super.visitAnnotation(Type.getDescriptor(Controller.class), true);
                    av.visitEnd();
                }else {
                    throw new EventException(ES_RPC_025);
                }
            }
        }, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    private void registerClassWithAnnotations(String beanName,Class classBytes, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(classBytes);
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE); // 设置类的角色为控制器
        if(registry.containsBeanDefinition(beanName)){
            registry.removeBeanDefinition(beanName);
        }
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    private static class InterfaceAnnotationTypeFilter extends AbstractTypeHierarchyTraversingFilter {

        private final Class<? extends java.lang.annotation.Annotation> annotationType;

        public InterfaceAnnotationTypeFilter(Class<? extends java.lang.annotation.Annotation> annotationType) {
            super(false, true);
            this.annotationType = annotationType;
        }

        @Override
        protected Boolean matchInterface(String interfaceName) {
            try {
                Class<?> interfaceClass = Class.forName(interfaceName);
                return interfaceClass.isAnnotationPresent(annotationType);
            } catch (ClassNotFoundException e) {
                throw new EventException(ES_RPC_018);
            }
        }

        @Override
        protected boolean matchClassName(String className) {
            return false;
        }
    }



    private static byte[] addMessageMappingToImplementedMethods(Class<?> clazz,String serviceName) throws IOException {
        // 获取接口上带有 @RSocketRpcDiscorvery 注解的方法列表
        Set<Method> interfaceMethods = getInterfaceMethodsAnnotatedWithRSocketRpcDiscovery(clazz);
        // 遍历类中的所有方法
        InputStream inputStream = clazz.getResourceAsStream(clazz.getSimpleName() + ".class");
        Assert.verify(inputStream,ES_RPC_018);
        ClassReader classReader = new ClassReader(inputStream);
        logger.info("ASM开始执行addMessageMappingToImplementedMethods.classReader");
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        classReader.accept(new ClassVisitor(ASM7, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                // 检查类方法是否与接口方法匹配，如果匹配则在类方法上添加 @MessageMapping 注解
                for (Method interfaceMethod : interfaceMethods) {
                    if (isMethodMatching(name, descriptor, interfaceMethod)) {
                        mv = new MethodVisitor(ASM7, mv) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                                if (desc.equals(Type.getDescriptor(MessageMapping.class))) {
                                    throw new EventException(ES_RPC_024);
                                }
                                return super.visitAnnotation(desc, visible);
                            }


                            @Override
                            public void visitCode() {
                                super.visitCode();
                                // 在方法代码的开头添加 @MessageMapping 注解
                                AnnotationVisitor av = super.visitAnnotation(Type.getDescriptor(MessageMapping.class), true);
                                // 访问注解的value属性
                                AnnotationVisitor valueAv = av.visitArray("value");
                                // 设置数组元素值
                                String route = buildMessageMapping(serviceName,interfaceMethod.getDeclaringClass(),interfaceMethod);

                                valueAv.visit(null, route);
                                // 如果您有多个值，可以重复上面的步骤
                                // valueAv.visit(null, "/another/rpc/test");
                                valueAv.visitEnd(); // 结束数组访问
                                av.visitEnd(); // 结束注解访问
                            }
                        };
                        break;
                    }
                }
                return mv;
            }
        }, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }
    private static Set<Method> getInterfaceMethodsAnnotatedWithRSocketRpcDiscovery(Class<?> clazz) {
        // 获取类实现的所有接口
        Class<?>[] interfaces = clazz.getInterfaces();
        Set<Method> annotatedMethods = new HashSet<>();
        // 遍历接口
        for (Class<?> interfaze : interfaces) {
            // 检查接口是否带有 @RSocketRpcDiscorvery 注解
            if (interfaze.isAnnotationPresent(RSocketRpcDiscorvery.class)) {
                // 添加带有 @RSocketRpcDiscorvery 注解的接口中的所有方法
                annotatedMethods.addAll(Arrays.asList(interfaze.getDeclaredMethods()));
            }
        }
        return annotatedMethods;
    }

    private static boolean isMethodMatching(String methodName, String methodDescriptor, Method interfaceMethod) {
        // 检查方法名和参数类型是否匹配
        return interfaceMethod.getName().equals(methodName) &&
                Type.getMethodDescriptor(interfaceMethod).equals(methodDescriptor);
    }



}
