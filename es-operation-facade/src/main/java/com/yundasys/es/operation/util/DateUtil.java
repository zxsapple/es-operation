package com.yundasys.es.operation.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {


    public static String getStringByPattern(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }
}