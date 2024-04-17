package org.lambda.framework.compliance.po;

import java.util.List;

public interface IFlattenTreePO<ID> {
    public ID getId();

    public void setId(ID id);

    public ID getParentId();

    public void setParentId(ID parentId);

    public ID getOrganizationId();

    public void setOrganizationId(ID organizationId);

    public <PO extends IFlattenTreePO<ID>>List<PO> getChildrens();

    public <PO extends IFlattenTreePO<ID>>void setChildrens(List<PO> childrens);
}
