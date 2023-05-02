package org.lambda.framework.web.security.manger.support;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class SecurityRole implements Serializable {
    private Long id;
    private String name;

    private List<SecurityPermission> securityPermissions;
}
