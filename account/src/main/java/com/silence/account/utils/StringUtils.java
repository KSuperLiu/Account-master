package com.silence.account.utils;

/**
 *
 */
public class StringUtils {
    private StringUtils() {
    }

    public static boolean checkEmail(String email) {
        String regex = "^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        return email.matches(regex);
    }
}
