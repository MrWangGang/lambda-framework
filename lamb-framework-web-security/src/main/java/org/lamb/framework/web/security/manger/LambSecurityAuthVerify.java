package org.lamb.framework.web.security.manger;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

public interface LambSecurityAuthVerify {
    public boolean verify(String principal);

    public Mono<Authentication> authenticate(AuthorizationContext authorizationContext);
}
