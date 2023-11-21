package org.lambda.framework.compliance.repository.po;

import java.util.List;

public interface IFlattenTreePO {
    public Long getId();

    public void setId(Long id);

    public Long getParentId();

    public void setParentId(Long parentId);

    public Long getOrganizationId();

    public void setOrganizationId(Long organizationId);

    public <PO extends IFlattenTreePO>List<PO> getChildrens();

    public <PO extends IFlattenTreePO>void setChildrens(List<PO> childrens);
}
