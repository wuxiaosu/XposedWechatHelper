package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;

import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import java.lang.reflect.Method;
import java.util.Random;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by su on 2018/02/06.
 * 骰子 猜拳 hook
 */

public class EmojiGameHook {

    private boolean fakeMorra;
    private String morra;
    private boolean fakeDice;
    private String dice;

    private EmojiGameHook() {
    }

    private ClassLoader classLoader;

    public void init(ClassLoader classLoader, String versionName) {
        if (this.classLoader == null) {
            this.classLoader = classLoader;
            hook(classLoader);
        }
    }

    public static EmojiGameHook getInstance() {
        return EmojiGameHook.SingletonHolder.instance;
    }

    private static class SingletonHolder {
        @SuppressLint("StaticFieldLeak")
        private static final EmojiGameHook instance = new EmojiGameHook();
    }

    private void hook(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(Random.class, "nextInt", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if ((int) param.args[0] < 10) {
                        StackTraceElement[] traceElements = Thread.currentThread().getStackTrace();
                        for (StackTraceElement traceElement : traceElements) {
                            if (traceElement.getClassName().startsWith("com.tencent.mm.sdk.platformtools.")) {
                                Method[] methods = XposedHelpers.findClass(traceElement.getClassName(), classLoader).getMethods();
                                for (Method method : methods) {
                                    if (method.getName().equals(traceElement.getMethodName())) {
                                        if (method.getParameterTypes().length == 2) {
                                            if (method.getParameterTypes()[0] == int.class
                                                    && method.getParameterTypes()[1] == int.class
                                                    && method.getReturnType() == int.class) {
                                                hook(classLoader, traceElement.getClassName(), method.getName());
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    super.beforeHookedMethod(param);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hook(final ClassLoader classLoader, String clazzName, String methodName) {
        try {
            Class clazz = XposedHelpers.findClass(clazzName, classLoader);
            XposedHelpers.findAndHookMethod(clazz, methodName, int.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int result = (int) param.getResult();
                    int type = (int) param.args[0];
                    reload();
                    switch (type) {
                        case 5:
                            if (fakeDice) {
                                result = Integer.valueOf(dice);
                            }
                            break;
                        case 2:
                            if (fakeMorra) {
                                result = Integer.valueOf(morra);
                            }
                            break;
                    }
                    param.setResult(result);
                    super.afterHookedMethod(param);
                }
            });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }

    private void reload() {
        fakeMorra = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "fake_morra", "false"));
        fakeDice = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "fake_dice", "false"));
        morra = PropertiesUtils.getValue(Constant.PRO_FILE, "morra", "0");
        dice = PropertiesUtils.getValue(Constant.PRO_FILE, "dice", "0");
    }
}
