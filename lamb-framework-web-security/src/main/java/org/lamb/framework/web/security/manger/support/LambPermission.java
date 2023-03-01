package org.lamb.framework.web.security.manger.support;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class LambPermission implements Serializable {
    private Long id;
    private String name;
}
