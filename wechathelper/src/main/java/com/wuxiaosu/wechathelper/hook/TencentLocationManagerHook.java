package com.wuxiaosu.wechathelper.hook;

import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by su on 2017/8/30.
 * 腾讯定位 hook
 */

public class TencentLocationManagerHook {

    private static boolean fakeLocation;
    private static String latitude;
    private static String longitude;

    public static void hook(ClassLoader classLoader) {
        try {
            Class managerClazz = XposedHelpers.findClass("com.tencent.map.geolocation.TencentLocationManager", classLoader);
            XposedBridge.hookAllMethods(managerClazz, "requestLocationUpdates", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object tencentLocationListener = param.args[1];
                    for (Method method : tencentLocationListener.getClass().getDeclaredMethods()) {
                        if (method.getParameterTypes().length == 10) {
                            XposedBridge.hookAllMethods(tencentLocationListener.getClass(), method.getName(), new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    reload();
                                    if (fakeLocation) {
                                        param.args[1] = Double.valueOf(latitude);
                                        param.args[2] = Double.valueOf(longitude);
                                    }
                                    super.beforeHookedMethod(param);
                                }
                            });
                        }
                    }
                    super.beforeHookedMethod(param);
                }
            });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }


    private static void reload() {
        fakeLocation = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "fake_location", "false"));
        latitude = PropertiesUtils.getValue(Constant.PRO_FILE, "latitude", "39.908860");
        longitude = PropertiesUtils.getValue(Constant.PRO_FILE, "longitude", "116.397390");
    }
}
