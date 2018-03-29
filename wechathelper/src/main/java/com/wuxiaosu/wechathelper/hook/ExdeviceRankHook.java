package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by su on 2018/03/19.
 * 微信运动界面
 */

public class ExdeviceRankHook {

    private String itemInfoFieldName;
    private ListView listView;
    private int likeIconId = 0;

    private ExdeviceRankHook() {
    }

    private ClassLoader classLoader;

    public void init(ClassLoader classLoader, String versionName) {
        switch (versionName) {
            case "6.6.0":
                itemInfoFieldName = "lqh";
                break;
            case "6.6.1":
                itemInfoFieldName = "lus";
                break;
            case "6.6.2":
                itemInfoFieldName = "lZj";
                break;
            case "6.6.3":
                itemInfoFieldName = "lZj";
                break;
            default:
            case "6.6.5":
                itemInfoFieldName = "meX";
                break;
        }
        if (this.classLoader == null) {
            this.classLoader = classLoader;
            hook(classLoader);
        }
    }

    public static ExdeviceRankHook getInstance() {
        return ExdeviceRankHookHolder.instance;
    }

    private static class ExdeviceRankHookHolder {
        @SuppressLint("StaticFieldLeak")
        private static final ExdeviceRankHook instance = new ExdeviceRankHook();
    }

    private void hook(final ClassLoader classLoader) {
        try {
            Class contextMenuClazz = XposedHelpers.findClass("com.tencent.mm.ui.base.n", classLoader);
            XposedHelpers.findAndHookMethod(contextMenuClazz, "a",
                    int.class, CharSequence.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if ((int) param.args[0] == 2 && param.args[1].equals("捐赠步数") && likeIconId == 0) {
                                likeIconId = (int) param.args[2];
                            }
                            super.beforeHookedMethod(param);
                        }
                    });
            Class menuClazz = XposedHelpers.findClass("com.tencent.mm.plugin.exdevice.ui.ExdeviceRankInfoUI$19", classLoader);

            XposedBridge.hookAllMethods(menuClazz, "a", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object object = param.args[0];
                    int menuSize = (int) XposedHelpers.callMethod(object, "size");
                    if (menuSize <= 4) {
                        XposedHelpers.callMethod(object,
                                "a", 4, "一键点赞", likeIconId);
                    }
                    super.afterHookedMethod(param);
                }
            });

            Class listenerClazz = XposedHelpers.findClass("com.tencent.mm.plugin.exdevice.ui.ExdeviceRankInfoUI$20", classLoader);

            XposedHelpers.findAndHookMethod(listenerClazz, "onMMMenuItemSelected", MenuItem.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    MenuItem menuItem = (MenuItem) param.args[0];
                    if (menuItem.getItemId() == 4 && listView != null) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ListAdapter listAdapter = listView.getAdapter();
                                int rankNum = (int) XposedHelpers.getObjectField(
                                        XposedHelpers.getObjectField(listAdapter.getItem(1),
                                                itemInfoFieldName),
                                        "field_ranknum");
                                for (int i = 3; i < listAdapter.getCount() - 1; i++) {
                                    if (i != rankNum + 2) {
                                        int selfLikeState = (int) XposedHelpers.getObjectField(
                                                XposedHelpers.getObjectField(listAdapter.getItem(i),
                                                        itemInfoFieldName),
                                                "field_selfLikeState");
                                        if (selfLikeState == 0) {
                                            final View view = ((RelativeLayout) ((LinearLayout)
                                                    ((RelativeLayout) listAdapter.getView(i, null, null))
                                                            .getChildAt(1))
                                                    .getChildAt(1)).getChildAt(1);

                                            ((Activity) listView.getContext()).runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    view.performClick();
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        }).start();
                    }
                    super.afterHookedMethod(param);
                }
            });

            final Class exdeviceRankInfoUIClazz = XposedHelpers.findClass("com.tencent.mm.plugin.exdevice.ui.ExdeviceRankInfoUI", classLoader);
            XposedHelpers.findAndHookMethod(exdeviceRankInfoUIClazz, "initView", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    listView = (ListView) XposedHelpers.callStaticMethod(exdeviceRankInfoUIClazz,
                            "q", param.thisObject);
                    super.afterHookedMethod(param);
                }
            });

        } catch (Error | Exception e) {
            XposedBridge.log(e);
        }
    }
}
