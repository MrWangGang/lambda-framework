package org.lamb.framework.web.security.manger.support;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class LambRole implements Serializable {
    private Long id;
    private String name;

    private List<LambPermission> permissions;
}
