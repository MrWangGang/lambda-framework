package org.lambda.framework.common.annotation.rsocket;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a Spring-managed implementation of a Java RSocket RPC Facade. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
public @interface RSocketFacade {
    @AliasFor(annotation = Controller.class)
    String value() default "";
}
