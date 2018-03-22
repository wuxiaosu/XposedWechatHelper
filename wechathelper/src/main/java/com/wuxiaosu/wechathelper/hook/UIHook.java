package com.wuxiaosu.wechathelper.hook;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wuxiaosu.wechathelper.BuildConfig;
import com.wuxiaosu.widget.SettingLabelView;

import java.util.HashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by su on 2018/03/16.
 */

public class UIHook {
    private XSharedPreferences xsp;

    private boolean hideDiscover;
    private String wClassName;
    private String wFieldName;
    private HashMap wHashMap = new HashMap();

    private int[] iconId;

    private String discoverFragmentName;
    private String myFragmentName;
    private String adapterClassName;
    private String vMethodName;


    public UIHook(String versionName) {
        switch (versionName) {
            case "6.6.0":
                iconId = new int[]{2131165901, 2131165904, 2131165905, 2131165906};
                wClassName = "com.tencent.mm.ui.x";
                wFieldName = "wHJ";
                wHashMap.put("Lcom/tencent/mm/modelsns/e;", 0);
                wHashMap.put("Lcom/tencent/mm/modelsns/b;", 1);
                wHashMap.put("Lcom/tencent/mm/modelsns/d;", 2);
                discoverFragmentName = "com.tencent.mm.ui.i";
                myFragmentName = "com.tencent.mm.ui.z";
                adapterClassName = "com.tencent.mm.ui.x$a";
                vMethodName = "ES";
                break;
            case "6.6.1":
                iconId = new int[]{2131165910, 2131165913, 2131165914, 2131165915};
                wClassName = "com.tencent.mm.ui.x";
                wFieldName = "wMd";
                wHashMap.put("Lcom/tencent/mm/modelvideo/MMVideoView;", 0);
                wHashMap.put("Lcom/tencent/mm/modelvideo/MMVideoView$1;", 1);
                wHashMap.put("Lcom/tencent/mm/modelvideo/MMVideoView$a;", 2);
                discoverFragmentName = "com.tencent.mm.ui.i";
                myFragmentName = "com.tencent.mm.ui.z";
                adapterClassName = "com.tencent.mm.ui.x$a";
                vMethodName = "Fb";
                break;
            case "6.6.2":
                iconId = new int[]{2131165910, 2131165913, 2131165914, 2131165915};
                wClassName = "com.tencent.mm.ui.w";
                wFieldName = "xKG";
                wHashMap.put("Lcom/google/android/gms/analytics/internal/u$a;", 0);
                wHashMap.put("Lcom/google/android/gms/analytics/internal/s;", 1);
                wHashMap.put("Lcom/google/android/gms/analytics/internal/t$a;", 2);
                discoverFragmentName = "com.tencent.mm.ui.h";
                myFragmentName = "com.tencent.mm.ui.y";
                adapterClassName = "com.tencent.mm.ui.w$a";
                vMethodName = "xe";
                break;
            case "6.6.3":
                iconId = new int[]{2131165910, 2131165913, 2131165914, 2131165915};
                wClassName = "com.tencent.mm.ui.w";
                wFieldName = "xKG";
                wHashMap.put("Lcom/google/android/gms/common/j$aa;", 0);
                wHashMap.put("Lcom/google/android/gms/common/internal/zzab;", 1);
                wHashMap.put("Lcom/google/android/gms/common/j$aa$1;", 2);
                discoverFragmentName = "com.tencent.mm.ui.h";
                myFragmentName = "com.tencent.mm.ui.y";
                adapterClassName = "com.tencent.mm.ui.w$a";
                vMethodName = "xe";
                break;
            default:
            case "6.6.5":
                iconId = new int[]{2131165910, 2131165913, 2131165914, 2131165915};
                wClassName = "com.tencent.mm.ui.w";
                wFieldName = "xTl";
                wHashMap.put("Lcom/google/android/exoplayer2/h/r;", 0);
                wHashMap.put("Lcom/google/android/exoplayer2/h/r$c;", 1);
                wHashMap.put("Lcom/google/android/exoplayer2/h/r$e;", 2);
                discoverFragmentName = "com.tencent.mm.ui.h";
                myFragmentName = "com.tencent.mm.ui.y";
                adapterClassName = "com.tencent.mm.ui.w$a";
                vMethodName = "xw";
                break;
        }
    }

    public void hook(final ClassLoader classLoader) {
        xsp = new XSharedPreferences(BuildConfig.APPLICATION_ID, SettingLabelView.DEFAULT_PREFERENCES_NAME);
        xsp.makeWorldReadable();
        hideDiscover = xsp.getBoolean("hide_discover", false);

        if (!hideDiscover) {
            return;
        }

        try {
            Class clazz = XposedHelpers.findClass(wClassName, classLoader);
            XposedHelpers.setStaticObjectField(clazz, wFieldName, wHashMap);

            // replace fragment
            Class baseBundleClazz = XposedHelpers.findClass("android.os.BaseBundle", classLoader);
            XposedHelpers.findAndHookMethod(baseBundleClazz, "putInt", String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[0].equals(discoverFragmentName)) {
                        param.args[0] = myFragmentName;
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
                                param.args[1] = myFragmentName;
                            }
                            super.beforeHookedMethod(param);
                        }
                    });

            Class adapterClass = XposedHelpers.findClass(adapterClassName, classLoader);

            XposedHelpers.findAndHookMethod(adapterClass, "getCount", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return 3;
                }
            });

            Class viewPagerClass = XposedHelpers.findClass("com.tencent.mm.ui.mogic.WxViewPager", classLoader);

            XposedHelpers.findAndHookMethod(viewPagerClass, vMethodName, int.class, new XC_MethodHook() {
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

        try {
            Class clazz = XposedHelpers.findClass("com.tencent.mm.ui.TabIconView", classLoader);
            XposedHelpers.findAndHookMethod(clazz, "g",
                    int.class, int.class, int.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if ((int) param.args[0] == iconId[0]) {
                                param.args[0] = iconId[1];
                                param.args[1] = iconId[2];
                                param.args[2] = iconId[3];
                            }
                            super.beforeHookedMethod(param);
                        }
                    });
        } catch (Error | Exception e) {
            XposedBridge.log(e);
        }
    }
}
