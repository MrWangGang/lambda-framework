package org.lambda.framework.web.security.container;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * @description: 框架层的用户接口
 * @author: Mr.WangGang
 * @create: 2018-11-30 下午 4:28
 **/
@Data
@Builder
public class AuthToken extends AbstractAuthenticationToken implements Authentication {
    private final String principal;

    private String credentials;

    private boolean authenticated = false;


    public AuthToken(String principal, String credentials, boolean authenticated){
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        this.authenticated = authenticated;
    }

}
