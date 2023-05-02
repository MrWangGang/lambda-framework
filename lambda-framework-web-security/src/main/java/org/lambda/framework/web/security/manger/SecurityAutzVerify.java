package org.lambda.framework.web.security.manger;
@FunctionalInterface
public interface SecurityAutzVerify {
    public boolean verify(String currentPathAutzTree,String principal);
}
