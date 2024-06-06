package org.lambda.framework.rpc.adapter.support;

import org.lambda.framework.common.exception.EventException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.lambda.framework.rpc.enums.RpcExceptionEnum.ES_RPC_018;

public class CustomClassLoader  extends ClassLoader {

    public Class<?> loadClass(byte[] classBytes) throws ClassNotFoundException {
        InputStream inputStream = new ByteArrayInputStream(classBytes);
        byte[] buffer = new byte[classBytes.length];
        int bytesRead;
        try {
            bytesRead = inputStream.read(buffer);
            if (bytesRead != -1) {
                // 读取到数据，定义类并返回
                return defineClass(null, buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            throw new EventException(ES_RPC_018);
        }
        // 如果没有读取到数据或者出现异常，则抛出ClassNotFoundException
        throw new EventException(ES_RPC_018);
    }
}
