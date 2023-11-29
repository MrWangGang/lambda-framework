package org.lambda.framework.security.container;

import lombok.Data;

@Data
public class LambdaSecurityAuthToken<T extends SecurityLoginUser> {
    private String principal;
    private String token;
}
