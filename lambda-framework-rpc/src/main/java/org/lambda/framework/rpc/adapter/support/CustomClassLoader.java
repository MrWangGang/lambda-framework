package org.lambda.framework.rpc.adapter.support;

import org.apache.commons.logging.LogFactory;
import org.lambda.framework.common.exception.EventException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import static org.lambda.framework.rpc.enums.RpcExceptionEnum.ES_RPC_018;

public class CustomClassLoader  extends ClassLoader {
    private static final Logger logger = Logger.getLogger(LogFactory.class.getName());
    public CustomClassLoader() {
        // 将应用程序类加载器（系统类加载器）作为父加载器
        super(CustomClassLoader.class.getClassLoader());
    }
    public Class<?> loadClass(byte[] classBytes) throws ClassNotFoundException {
        logger.info("java.classpath>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+System.getProperty("java.class.path"));
        logger.info("开始读取classBytes>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        InputStream inputStream = new ByteArrayInputStream(classBytes);
        byte[] buffer = new byte[classBytes.length];
        int bytesRead;
        try {
            bytesRead = inputStream.read(buffer);
            if (bytesRead != -1) {
                // 读取到数据，定义类并返回
                return defineClass(null, buffer, 0, bytesRead);
            }
            logger.info("读取classBytes结束>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        } catch (Exception e) {
            throw new EventException(ES_RPC_018);
        }
        // 如果没有读取到数据或者出现异常，则抛出ClassNotFoundException
        throw new EventException(ES_RPC_018);
    }
}
