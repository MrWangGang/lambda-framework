package org.lambda.framework.rpc.adapter.support;

public class CamelCase {
    public static String xtoCamelCase(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
}
