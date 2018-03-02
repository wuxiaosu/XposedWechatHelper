package com.wuxiaosu.wechathelper.hook;

import com.wuxiaosu.wechathelper.BuildConfig;
import com.wuxiaosu.widget.SettingLabelView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by su on 2018/02/07.
 * 运动步数
 */

public class StepHook {
    private static XSharedPreferences xsp;
    private static boolean fakeStep;
    private static String step;

    public static void hook(ClassLoader classLoader) {
        xsp = new XSharedPreferences(BuildConfig.APPLICATION_ID, SettingLabelView.DEFAULT_PREFERENCES_NAME);
        xsp.makeWorldReadable();

        try {
            //4.4 nexus 通过
            Class clazz = XposedHelpers.findClass("android.hardware.SystemSensorManager$SensorEventQueue", classLoader);
            XposedBridge.hookAllMethods(clazz, "dispatchSensorEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    reload();
                    if (fakeStep) {
                        ((float[]) param.args[1])[0] = (((float[]) param.args[1])[0]) * Integer.valueOf(step);
                    }
                    super.beforeHookedMethod(param);
                }

            });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }


    private static void reload() {
        xsp.reload();
        fakeStep = xsp.getBoolean("fake_step", false);
        step = xsp.getString("step", "2");
    }
}
