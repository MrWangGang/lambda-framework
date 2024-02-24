package org.lambda.framework.compliance.security.container;

import lombok.Data;

import java.io.Serializable;

@Data
public class LambdaSecurityAuthToken<T extends SecurityLoginUser<?>> implements Serializable {
    private String principal;
    private String token;
}
