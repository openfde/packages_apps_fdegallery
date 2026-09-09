package com.fde.imageeditlibrary.editimage.utils;

import java.lang.reflect.Method;

public class Utils {
    public static int ToInt(Object ojb) {
        if (ojb == null) {
            return 0;
        } else {
            return ToDouble(ojb).intValue();
        }
    }

    public static Double ToDouble(Object ojb) {
        if (ojb == null) {
            return 0.0;
        } else {
            return Double.valueOf(ToString(ojb));
        }
    }

    public static String ToString(Object ojb) {
        if (ojb == null) {
            return "";
        } else {
            return String.valueOf(ojb).trim();
        }
    }

    public static String getSystemProperty(String key) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method get = sp.getMethod("get", String.class);
            return (String) get.invoke(null, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void setSystemProperty(String key, String value) {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method setMethod = systemPropertiesClass.getDeclaredMethod("set", String.class, String.class);
            setMethod.invoke(null, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
