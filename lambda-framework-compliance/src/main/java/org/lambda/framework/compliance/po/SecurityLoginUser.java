package org.lambda.framework.compliance.po;

public interface SecurityLoginUser<ID> {
    //主键

    public ID getId();
    public ID getOrganizationId();
    public String getName();
    public void setId(ID id);
    public void setOrganizationId(ID organizationId);
    public void setName(String name);
}
