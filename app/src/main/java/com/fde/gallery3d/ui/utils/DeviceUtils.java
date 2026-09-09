/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fde.gallery3d.ui.utils;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Method;


/**
 * Created by xudq on 16/5/11.
 */
public class DeviceUtils {

    public static int getSreenWidth(Context context) {

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        Display defaultDisplay = wm.getDefaultDisplay();

        return defaultDisplay.getWidth();

    }

    public static float getSreenDensity(Activity baseActivity) {
        DisplayMetrics dm = new DisplayMetrics();
        baseActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.density;
    }

    public static int getShowCount(Activity baseActivity) {
        int w = getSreenWidth(baseActivity);
        float d = getSreenDensity(baseActivity);
        float s = w / d;
        if (s < 430) {
            return 4;
        } else if (s < 860) {
            return 6;
        } else {
            return 8;
        }
    }

    public static int getSreenHight(Context context) {

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        Display defaultDisplay = wm.getDefaultDisplay();

        return defaultDisplay.getHeight();

    }

    public static int getScaleHeight(Context context, int width, int height) {

        return getSreenWidth(context) * height / width;

    }

    public static void toggleKeyboard(Context context) {

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);

    }

    //
    public static void hideKeyboard(Activity act) {

        InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromInputMethod(act.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);


    }

    //弹出键盘
    public static void showKeyboard(Context context, View view) {

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);

    }


    public static void sendKeyEvent(final int KeyCode) {
        new Thread() { // 不可在主线程中调用
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(KeyCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }.start();
    }

//    public static int getScaleWidth(Context context,int height){
//
//    }

    /**
     * @param bgAlpha
     */
    public static void backgroundAlpha(Activity act, float bgAlpha) {
        WindowManager.LayoutParams lp = act.getWindow().getAttributes();
        lp.alpha = bgAlpha; //0.0-1.0
        act.getWindow().setAttributes(lp);
    }

    public static String getAppVersionName(Context context) {

        String versionName = "";

        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
            if (TextUtils.isEmpty(versionName)) {
                return "";
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    public static int getAppVersionCode(Context context) {
        int versionCode = 0;

        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionCode = packageInfo.versionCode;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;

    }

    public static int dpToPx(Context context,int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    public static  void setBottomMargin(View view, int dpValue) {
        float density = view.getResources().getDisplayMetrics().density;
        int pxValue = (int) (dpValue * density + 0.5f);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.bottomMargin = pxValue;
        view.setLayoutParams(params);
    }

}
