package org.lambda.framework.security.manger.support;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class SecurityPermission implements Serializable {
    private Long id;
    private String name;
}
