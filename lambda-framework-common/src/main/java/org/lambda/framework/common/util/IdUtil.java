package org.lambda.framework.common.util;

import java.util.UUID;

public final class IdUtil {
    private IdUtil() {}

    public static String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
