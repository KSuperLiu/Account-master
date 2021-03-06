package com.silence.account.utils;

import java.text.DecimalFormat;

/**
 * 格式化数字工具类
 *
 */
public class FormatUtils {

    private FormatUtils() {
    }

    public static float formatFloat(String pattern, float value) {
        DecimalFormat decimalFormat = new DecimalFormat(pattern);
        return Float.parseFloat(decimalFormat.format(value));
    }
}
