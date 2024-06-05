package org.lambda.framework.rpc.adapter.support;

public class CustomClassLoader  extends ClassLoader {
    public Class<?> defineClassFromBytes(String className, byte[] classBytes) {
        return defineClass(className, classBytes, 0, classBytes.length);
    }
}
