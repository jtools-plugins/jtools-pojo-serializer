package com.lhstack.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ExceptionUtils {

    public static String extraStackMsg(Throwable e) {
        return e + "\r\n" + Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\r\n"));
    }
}
