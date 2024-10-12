package org.lambda.framework.common.util.sample;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import java.util.Random;

/**
 * Created by WangGang on 2017/7/10 0010.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public class NanoIdUtil {

    public static String get(char[] alphabet, int size) {
        return NanoIdUtils.randomNanoId(new Random(), alphabet, size);
    }
}
