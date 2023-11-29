package org.lambda.framework.security.manger;

import org.lambda.framework.security.container.SecurityAuthToken;
import org.lambda.framework.security.container.SecurityLoginUser;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

public interface SecurityAuthVerify {
    public boolean verify(String principal);

    public Mono<SecurityAuthToken> authenticate(AuthorizationContext authorizationContext);
}
