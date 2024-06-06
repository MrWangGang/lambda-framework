package org.lambda.framework.rpc.adapter.support;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CamelCase {
    public static String xtoCamelCase(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
    public static String buildMessageMapping(String serviceName, Class<?> interfaceField, Method method) {


        String route =processServiceName(serviceName)+"."+xtoCamelCase(interfaceField.getSimpleName())+"."+method.getName();
        return route;
    }

    public static String processServiceName(String serviceName) {
        // 使用正则表达式检查是否是IP地址加端口的形式
        Pattern pattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+");
        Matcher matcher = pattern.matcher(serviceName);

        // 如果是IP地址加端口的形式，则替换冒号为点号
        if (matcher.matches()) {
            return serviceName.replace(":", ".");
        } else {
            // 如果不是，则返回原字符串
            return serviceName;
        }
    }
}
