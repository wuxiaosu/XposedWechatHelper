package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;

import com.wuxiaosu.wechathelper.BuildConfig;
import com.wuxiaosu.widget.SettingLabelView;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

/**
 * Created by su on 2018/04/12.
 * 首页
 */

public class LauncherUIHook {

    private XSharedPreferences xsp;

    private boolean fakeLauncherMenu;

    private int[] iconIds;

    private LauncherUIHook() {
    }

    private ClassLoader classLoader;

    public void init(ClassLoader classLoader, String versionName) {
        switch (versionName) {
            case "6.6.0":
                iconIds = new int[]{2131165958, 2131166039};
                break;
            case "6.6.1":
                iconIds = new int[]{2131165967, 2131166048};
                break;
            case "6.6.2":
                iconIds = new int[]{2131165967, 2131166048};
                break;
            case "6.6.3":
                iconIds = new int[]{2131165967, 2131166048};
                break;
            case "6.6.5":
                iconIds = new int[]{2131165967, 2131166048};
                break;
            default:
            case "6.6.6":
                iconIds = new int[]{2131165986, 2131166064};
                break;
        }
        if (this.classLoader == null) {
            this.classLoader = classLoader;
            hook(classLoader);
        }
    }

    public static LauncherUIHook getInstance() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        @SuppressLint("StaticFieldLeak")
        private static final LauncherUIHook instance = new LauncherUIHook();
    }

    private void hook(final ClassLoader classLoader) {
        xsp = new XSharedPreferences(BuildConfig.APPLICATION_ID, SettingLabelView.DEFAULT_PREFERENCES_NAME);
        xsp.makeWorldReadable();
        fakeLauncherMenu = xsp.getBoolean("fake_launcher_menu", false);

        if (!fakeLauncherMenu) {
            return;
        }

        try {
            Class clazz = XposedHelpers.findClass("com.tencent.mm.ui.LauncherUI", classLoader);
            XposedHelpers.findAndHookMethod(clazz, "onCreateOptionsMenu",
                    Menu.class, new XC_MethodHook() {
                        @SuppressLint("ResourceType")
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Menu menu = (Menu) param.args[0];
                            MenuItem menuItem = menu.add(0, 3, 0, "扫一扫");
                            menuItem.setIcon(iconIds[0]);
                            menuItem.setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS);
                            menuItem = menu.add(0, 4, 0, "收付款");
                            menuItem.setIcon(iconIds[1]);
                            menuItem.setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS);
                            menu.removeItem(2);
                            super.afterHookedMethod(param);
                        }
                    });

            XposedHelpers.findAndHookMethod(clazz, "onOptionsItemSelected",
                    MenuItem.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            MenuItem item = (MenuItem) param.args[0];
                            if (item.getItemId() == 3) {
                                Intent intent = new Intent();
                                intent.setClassName("com.tencent.mm",
                                        "com.tencent.mm.plugin.scanner.ui.BaseScanUI");
                                intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                                XposedHelpers.callMethod(param.thisObject,
                                        "startActivity", intent);
                            } else if (item.getItemId() == 4) {
                                Intent intent = new Intent();
                                intent.putExtra("key_from_scene", 2);
                                intent.setClassName("com.tencent.mm",
                                        "com.tencent.mm.plugin.offline.ui.WalletOfflineCoinPurseUI");
                                intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                                XposedHelpers.callMethod(param.thisObject,
                                        "startActivity", intent);
                            }
                            super.afterHookedMethod(param);
                        }
                    });
        } catch (Error | Exception e) {
            XposedBridge.log(e);
        }
    }
}
