package org.lambda.framework.security.manger;
public interface SecurityAutzVerify {
    public boolean verify(String currentPathAutzTree,String principal);
}
