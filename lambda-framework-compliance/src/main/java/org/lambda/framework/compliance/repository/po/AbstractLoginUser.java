package org.lambda.framework.compliance.repository.po;

import lombok.Data;
import org.lambda.framework.security.container.SecurityLoginUser;

@Data
public class AbstractLoginUser implements SecurityLoginUser {

    private Long id;

    private Long organizationId;

    private String name;
}
