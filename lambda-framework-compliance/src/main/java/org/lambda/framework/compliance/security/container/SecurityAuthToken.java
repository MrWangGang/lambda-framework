package org.lambda.framework.compliance.security.container;

import lombok.Builder;
import lombok.Data;

/**
 * @description: 框架层的用户接口
 * @author: Mr.WangGang
 * @create: 2018-11-30 下午 4:28
 **/
@Data
@Builder
public class SecurityAuthToken  {
    private final String principal;

    private String credentials;

    private boolean authenticated = false;


    public SecurityAuthToken(String principal, String credentials, boolean authenticated){
        this.principal = principal;
        this.credentials = credentials;
        this.authenticated = authenticated;
    }

}
