package org.lambda.framework.web.security.manger;
@FunctionalInterface
public interface AutzVerify {
    public boolean verify(String currentPathAutzTree,String principal);
}
