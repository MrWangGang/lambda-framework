package org.lambda.framework.rpc.annotation.rsocket;

import jakarta.annotation.Resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Resource
public @interface RsocketRpc {

}
