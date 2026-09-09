package com.fde.gallery3d.ui.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.fde.gallery3d.ui.common.Constant;


public class SPUtils {
    public static String getUserInfo(Context context, String key) {
        SharedPreferences shared_user_info = context.getSharedPreferences(Constant.SP_STR, context.MODE_PRIVATE);
        return shared_user_info.getString(key, "");
    }

    public static void putUserInfo(Context context, String key, String values) {
        SharedPreferences shared_user_info = context.getSharedPreferences(Constant.SP_STR, context.MODE_PRIVATE);
        shared_user_info.edit().putString(key, values).commit();
    }

    public static void cleanUserInfo(Context context) {
        SharedPreferences shared_user_info = context.getSharedPreferences(Constant.SP_STR, context.MODE_PRIVATE);
        shared_user_info.edit().clear().commit();
    }

}
