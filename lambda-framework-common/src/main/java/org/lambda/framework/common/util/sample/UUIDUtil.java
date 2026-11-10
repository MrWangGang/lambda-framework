package org.lambda.framework.common.util.sample;

import org.lambda.framework.common.util.IdUtil;

/**
 * Created by WangGang on 2017/7/10 0010.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
@Deprecated
public class UUIDUtil {

    public static String get(){
        return IdUtil.compactUuid();
    }
}
