package com.wuxiaosu.wechathelper.hook;

import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by su on 2018/02/07.
 * 运动步数
 */

public class StepHook {
    private static boolean fakeStep;
    private static String step;

    public static void hook(ClassLoader classLoader) {

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
        fakeStep = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "fake_step", "false"));
        step = PropertiesUtils.getValue(Constant.PRO_FILE, "step", "2");
    }
}
