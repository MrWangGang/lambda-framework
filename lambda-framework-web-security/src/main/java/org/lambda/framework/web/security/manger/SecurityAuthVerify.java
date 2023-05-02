package org.lambda.framework.web.security.manger;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

public interface SecurityAuthVerify {
    public boolean verify(String principal);

    public Mono<Authentication> authenticate(AuthorizationContext authorizationContext);
}
