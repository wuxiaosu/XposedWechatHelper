package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static android.content.ContentValues.TAG;

/**
 * Created by su on 2018/03/16.
 */

public class UIHook {

    private boolean hideDiscover;

    private String discoverFragmentName;
    private String meFragmentName;

    private ClassLoader classLoader;

    private UIHook() {

    }

    public void init(ClassLoader classLoader, String versionName) {
        switch (versionName) {
            case "6.6.0":
                discoverFragmentName = "com.tencent.mm.ui.i";
                meFragmentName = "com.tencent.mm.ui.z";
                break;
            case "6.6.1":
                discoverFragmentName = "com.tencent.mm.ui.i";
                meFragmentName = "com.tencent.mm.ui.z";
                break;
            case "6.6.2":
                discoverFragmentName = "com.tencent.mm.ui.i";
                meFragmentName = "com.tencent.mm.ui.z";
                break;
            case "6.6.3":
                discoverFragmentName = "com.tencent.mm.ui.i";
                meFragmentName = "com.tencent.mm.ui.z";
                break;
            case "6.6.5":
                discoverFragmentName = "com.tencent.mm.ui.i";
                meFragmentName = "com.tencent.mm.ui.z";
                break;
            case "6.6.6":
                discoverFragmentName = "com.tencent.mm.ui.h";
                meFragmentName = "com.tencent.mm.ui.y";
                break;
            default:
            case "6.6.7":
                discoverFragmentName = "com.tencent.mm.ui.h";
                meFragmentName = "com.tencent.mm.ui.ab";
                break;
        }
        if (this.classLoader == null) {
            this.classLoader = classLoader;
            hook(classLoader);
        }
    }

    public static UIHook getInstance() {
        return UIHook.SingletonHolder.instance;
    }

    private static class SingletonHolder {
        @SuppressLint("StaticFieldLeak")
        private static final UIHook instance = new UIHook();
    }

    private void hook(final ClassLoader classLoader) {
        hideDiscover = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "hide_discover", "false"));

        if (!hideDiscover) {
            return;
        }

        initIconId(classLoader);
        initMainTabUI(classLoader);
        try {
            // replace fragment
            Class baseBundleClazz = XposedHelpers.findClass("android.os.BaseBundle", classLoader);
            XposedHelpers.findAndHookMethod(baseBundleClazz, "putInt", String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[0].equals(discoverFragmentName)) {
                        param.args[0] = meFragmentName;
                    }
                    super.beforeHookedMethod(param);
                }
            });
            Class fragmentClazz = XposedHelpers.findClass("android.support.v4.app.Fragment", classLoader);
            XposedHelpers.findAndHookMethod(fragmentClazz, "instantiate",
                    Context.class, String.class, Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args[1].equals(discoverFragmentName)) {
                                param.args[1] = meFragmentName;
                            }
                            super.beforeHookedMethod(param);
                        }
                    });

            Class viewPagerClass = XposedHelpers.findClass("com.tencent.mm.ui.mogic.WxViewPager", classLoader);

            XposedHelpers.findAndHookMethod(viewPagerClass, "setOffscreenPageLimit", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // tab count
                    param.args[0] = 3;
                    super.beforeHookedMethod(param);
                }
            });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }

        try {
            Class clazz = XposedHelpers.findClass("com.tencent.mm.ui.LauncherUIBottomTabView", classLoader);
            XposedBridge.hookAllMethods(clazz, "init", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    //隐藏“我”  将“发现”图标换成“我”
                    ViewGroup tebViewObject = (ViewGroup) param.thisObject;
                    RelativeLayout relativeLayout = (RelativeLayout)
                            ((LinearLayout) tebViewObject.getChildAt(0)).getChildAt(3);
                    relativeLayout.setVisibility(View.GONE);

                    relativeLayout = (RelativeLayout)
                            ((LinearLayout) tebViewObject.getChildAt(0)).getChildAt(2);
                    TextView textView = ((TextView) ((LinearLayout)
                            relativeLayout.getChildAt(0)).getChildAt(1));
                    textView.setText("我");
                    super.afterHookedMethod(param);
                }
            });
        } catch (Error | Exception e) {
            XposedBridge.log(e);
        }
    }

    private HashMap tempMap = new HashMap();

    private void initMainTabUI(final ClassLoader classLoader) {
        try {
            Class homeUIClazz = XposedHelpers.findClass("com.tencent.mm.ui.HomeUI", classLoader);
            Field[] fields = homeUIClazz.getFields();
            for (Field field : fields) {
                if (field.getType().getName().startsWith("com.tencent.mm.ui")) {
                    Class mainTabUIClazz = field.getType();

                    Field[] mainTabUIFields = mainTabUIClazz.getDeclaredFields();
                    for (Field mainTabUIField : mainTabUIFields) {
                        if (mainTabUIField.getType() == HashMap.class && Modifier.isStatic(mainTabUIField.getModifiers())) {
                            HashMap hashMap = (HashMap) XposedHelpers.getStaticObjectField(mainTabUIClazz, mainTabUIField.getName());
                            Object threeKey = null;
                            for (Object o : hashMap.keySet()) {
                                if ((int) hashMap.get(o) != 2) {
                                    if ((int) hashMap.get(o) == 3) {
                                        tempMap.put(threeKey, 2);
                                    } else {
                                        tempMap.put(o, hashMap.get(o));
                                    }
                                } else {
                                    threeKey = o;
                                }
                            }
                            XposedHelpers.setStaticObjectField(mainTabUIClazz, mainTabUIField.getName(), tempMap);
                            break;
                        }
                    }

                    handleHook(classLoader, mainTabUIClazz);
                    break;
                }
            }
        } catch (Error | Exception e) {
            XposedBridge.log(e);
        }
    }

    private void handleHook(final ClassLoader classLoader, Class mainTabUIClazz) {
        Method[] methods = mainTabUIClazz.getDeclaredMethods();
        for (Method method : methods) {

            if (method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0] == String.class
                    && method.getReturnType() == void.class) {

                XposedHelpers.findAndHookMethod(mainTabUIClazz, method.getName(),
                        String.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (!tempMap.containsKey(param.args[0])) {
                                    param.args[0] = tempMap.keySet().toArray()[0];
                                }
                                super.beforeHookedMethod(param);
                            }
                        });
            }
        }

        Class[] classes = mainTabUIClazz.getDeclaredClasses();
        // adapterClass
        XposedHelpers.findAndHookMethod(classes[0], "getCount", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return 3;
            }
        });
    }

    private boolean isThreeTabIcon = false;

    private void initIconId(final ClassLoader classLoader) {
        try {
            Class launcherUIBottomTabViewClazz = XposedHelpers.findClass("com.tencent.mm.ui.LauncherUIBottomTabView", classLoader);

            Method[] methods = launcherUIBottomTabViewClazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0] == int.class
                        && method.getReturnType() != void.class) {

                    XposedHelpers.findAndHookMethod(launcherUIBottomTabViewClazz, method.getName(),
                            int.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    isThreeTabIcon = ((int) param.args[0] == 2);
                                    super.afterHookedMethod(param);
                                }
                            });
                }
            }
            Class tabIconViewClazz = XposedHelpers.findClass("com.tencent.mm.ui.TabIconView", classLoader);

            Method[] tabIconViewMethods = tabIconViewClazz.getDeclaredMethods();
            for (Method tabIconViewMethod : tabIconViewMethods) {
                if (tabIconViewMethod.getParameterTypes().length == 4
                        && tabIconViewMethod.getParameterTypes()[0] == int.class
                        && tabIconViewMethod.getParameterTypes()[1] == int.class
                        && tabIconViewMethod.getParameterTypes()[2] == int.class
                        && tabIconViewMethod.getParameterTypes()[3] == boolean.class) {

                    XposedHelpers.findAndHookMethod(tabIconViewClazz, tabIconViewMethod.getName(),
                            int.class, int.class, int.class, boolean.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (isThreeTabIcon) {
                                        // 用“发现”页 iconId 递加，算出 “我” 页iconId
                                        param.args[0] = (int) param.args[0] + 3;
                                        param.args[1] = (int) param.args[1] + 3;
                                        param.args[2] = (int) param.args[2] + 3;
                                    }
                                    super.beforeHookedMethod(param);
                                }
                            });
                    break;
                }
            }
        } catch (Error | Exception e) {
            XposedBridge.log(e);
        }
    }
}
