package org.lambda.framework.security.manger;

import org.lambda.framework.compliance.security.container.SecurityAuthToken;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

public interface SecurityAuthVerify {
    public boolean verify(String principal);

    public Mono<SecurityAuthToken> authenticate(AuthorizationContext authorizationContext);
}
