package org.lamb.framework.web.security.manger;
@FunctionalInterface
public interface LambSecurityAutzVerify {
    public boolean verify(String currentPathAutzTree,String principal);
}
