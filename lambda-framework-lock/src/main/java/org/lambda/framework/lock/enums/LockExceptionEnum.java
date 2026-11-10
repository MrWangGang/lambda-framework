package org.lambda.framework.lock.enums;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum LockExceptionEnum implements ExceptionEnumFunction {
    ES_LOCK_000("S0900000", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_000("S0910000", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_001("S0910001", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_002("S0910002", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_003("S0910003", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_004("S0910004", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_005("S0910005", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_006("S0910006", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_007("S0910007", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_008("S0910008", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_009("S0910009", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_010("S0910010", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_011("S0910011", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_012("S0910012", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_013("S0910013", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_017("S0910017", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_018("S0910018", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_019("S0910019", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_020("S0910020", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_021("S0910021", "操作繁忙，请稍后重试"),
    ES_LOCK_REDISSON_022("S0910022", "操作繁忙，请稍后重试"),
    ES_LOCK_ZOOKEEPER_010("S0920010", "操作繁忙，请稍后重试"),
    ES_LOCK_ZOOKEEPER_011("S0920011", "操作繁忙，请稍后重试"),
    ES_LOCK_ZOOKEEPER_012("S0920012", "操作繁忙，请稍后重试"),
    ES_LOCK_ZOOKEEPER_013("S0920013", "操作繁忙，请稍后重试"),
    ES_LOCK_ZOOKEEPER_014("S0920014", "操作繁忙，请稍后重试"),
    ES_LOCK_ZOOKEEPER_015("S0920015", "操作繁忙，请稍后重试"),
    ES_LOCK_ZOOKEEPER_017("S0920017", "操作繁忙，请稍后重试"),
    ES_LOCK_ZOOKEEPER_018("S0920018", "操作繁忙，请稍后重试"),
    ES_LOCK_ZOOKEEPER_019("S0920019", "操作繁忙，请稍后重试"),
    ES_LOCK_ZOOKEEPER_020("S0920020", "操作繁忙，请稍后重试"),
    ES_LOCK_ZOOKEEPER_021("S0920021", "操作繁忙，请稍后重试");

    private final String code;
    private final String message;

    LockExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
