package org.lambda.framework.rpc.annotation.rsocket;

import org.springframework.util.MimeTypeUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RSocketRpcMapping {
    String value();
    String MimeType() default MimeTypeUtils.APPLICATION_JSON_VALUE;
}
