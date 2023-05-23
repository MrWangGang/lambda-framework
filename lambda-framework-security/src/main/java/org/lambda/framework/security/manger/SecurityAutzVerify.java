package org.lambda.framework.security.manger;
@FunctionalInterface
public interface SecurityAutzVerify {
    public boolean verify(String currentPathAutzTree,String principal);
}
