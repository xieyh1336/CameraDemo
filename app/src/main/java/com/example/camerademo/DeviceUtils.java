package com.example.camerademo;

import android.os.Build;
import android.util.Log;

public class DeviceUtils {
    // 小米
    public static final String PHONE_XIAOMI = "xiaomi";
    //红米
    public static final String PHONE_REDMI = "Redmi";
    // 华为
    public static final String PHONE_HUAWEI1 = "Huawei";
    // 华为
    public static final String PHONE_HUAWEI2 = "HUAWEI";
    // 华为
    public static final String PHONE_HUAWEI3 = "HONOR";
    // 魅族
    public static final String PHONE_MEIZU = "Meizu";
    // 索尼
    public static final String PHONE_SONY = "sony";
    // 三星
    public static final String PHONE_SAMSUNG = "samsung";
    // LG
    public static final String PHONE_LG = "lg";
    // HTC
    public static final String PHONE_HTC = "htc";
    // NOVA
    public static final String PHONE_NOVA = "nova";
    // OPPO
    public static final String PHONE_OPPO = "OPPO";
    // 乐视
    public static final String PHONE_LeMobile = "LeMobile";
    // 联想
    public static final String PHONE_LENOVO = "lenovo";

    public static String getDeviceBrand() {
        Log.e("123", "手机厂商：" + Build.BRAND);
        return Build.BRAND;
    }

    public static boolean isMIUI() {
        return getDeviceBrand().equals(PHONE_XIAOMI) || getDeviceBrand().equals(PHONE_REDMI);
    }
}
