package org.lambda.framework.compliance.repository.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UnifyPO {

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long creatorId;

    private Long updaterId;

    private String creatorName;

    private String updaterName;
}
