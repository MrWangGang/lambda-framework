package org.lambda.framework.compliance.repository.po;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UnifyPO implements Serializable {
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
