package org.lambda.framework.compliance.repository.po;

import java.util.List;

public interface IFlattenTreePO {
    public String getId();

    public void setId(String id);

    public String getParentId();

    public void setParentId(String parentId);

    public String getOrganizationId();

    public void setOrganizationId(String organizationId);

    public <PO extends IFlattenTreePO>List<PO> getChildrens();

    public <PO extends IFlattenTreePO>void setChildrens(List<PO> childrens);
}
