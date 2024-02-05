package org.lambda.framework.security.container;

public interface SecurityLoginUser {
    //主键

    public String getId();
    public String getOrganizationId();
    public String getName();
    public void setId(String id);
    public void setOrganizationId(String organizationId);
    public void setName(String name);
}
