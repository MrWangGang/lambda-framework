package org.lambda.framework.compliance.repository.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UnifyPO {

    @JsonIgnore
    private LocalDateTime createTime;
    @JsonIgnore
    private LocalDateTime updateTime;
    @JsonIgnore
    private Long creatorId;
    @JsonIgnore
    private Long updaterId;
    @JsonIgnore
    private String creatorName;
    @JsonIgnore
    private String updaterName;
}
