package com.wuxiaosu.wechathelper.hook;

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

    public ExdeviceRankHook(String versionName) {
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
    }

    public void hook(final ClassLoader classLoader) {
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
                        String nickName = "";
                        ListAdapter listAdapter = listView.getAdapter();
                        int count = listAdapter.getCount();
                        for (int i = 0; i < count; i++) {
                            Object item = listAdapter.getItem(i);
                            int type = listAdapter.getItemViewType(i);
                            if (type == 1 && item != null) {
                                String username = (String) XposedHelpers.getObjectField(
                                        XposedHelpers.getObjectField(item, itemInfoFieldName),
                                        "field_username");
                                if (i == 1) {
                                    nickName = username;
                                    continue;
                                }
                                if (!nickName.equals(username)) {
                                    View view = ((RelativeLayout) ((LinearLayout)
                                            ((RelativeLayout) listView.getChildAt(i))
                                                    .getChildAt(1))
                                            .getChildAt(1)).getChildAt(1);
                                    view.performClick();
                                    view.destroyDrawingCache();
                                }
                            }
                        }
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
