package org.lamb.framework.web.core.aspect;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.lamb.framework.common.exception.LambEventException;
import org.lamb.framework.common.util.sample.ValidatorUtil;
import org.lamb.framework.web.core.annotation.LambValid;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.lamb.framework.common.enums.LambExceptionEnum.ES00000099;

/**
 * @description: service层开启使用javax注解来校验参数
 * @author: Mr.WangGang
 * @create: 2018-11-23 下午 2:49
 **/
@Aspect
@Component
public class LambJavaxValidationServiceAspect {
    @Pointcut("@annotation(org.lamb.framework.web.core.annotation.LambValid)")
    public void serviceValid() {

    }
    @Before("serviceValid()")
    public void Interceptor(JoinPoint joinpoint) {
        MethodSignature methodSignature = (MethodSignature) joinpoint.getSignature();
        Method method = methodSignature.getMethod();
        LambValid lambValid = method.getDeclaredAnnotation(LambValid.class);
        if(lambValid == null){
            return;
        }
        Annotation[][] argAnnotations = method.getParameterAnnotations();
        Object[] args = joinpoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            for (Annotation annotation : argAnnotations[i]) {
                if (Valid.class.isInstance(annotation)) {
                    if(args[i] == null){
                        throw new LambEventException(ES00000099);
                    }
                    ValidatorUtil.validate(args[i]);
                }else if(NotNull.class.isInstance(annotation)){
                    if(args[i] == null){
                        throw new LambEventException(ES00000099);
                    }
                }else if(NotBlank.class.isInstance(annotation)){
                    if(args[i] == null){
                        throw new LambEventException(ES00000099);
                    }
                    if(StringUtils.isBlank((String)args[i])){
                        throw new LambEventException(ES00000099);
                    }
                }
            }
        }
    }

}
