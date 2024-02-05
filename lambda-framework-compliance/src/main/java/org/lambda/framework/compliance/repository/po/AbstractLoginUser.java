package org.lambda.framework.compliance.repository.po;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.lambda.framework.security.container.SecurityLoginUser;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbstractLoginUser implements SecurityLoginUser {

    private String id;

    private String organizationId;

    private String name;
}
