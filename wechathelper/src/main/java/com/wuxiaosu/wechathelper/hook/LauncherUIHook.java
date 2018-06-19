package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.PopupWindow;

import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

/**
 * Created by su on 2018/04/12.
 * 首页
 */

public class LauncherUIHook {

    private int[] iconIds = new int[2];

    private LauncherUIHook() {
    }

    private ClassLoader classLoader;

    public void init(ClassLoader classLoader, String versionName) {
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
        boolean fakeLauncherMenu = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "fake_launcher_menu", "false"));

        if (!fakeLauncherMenu) {
            return;
        }

        initItemClass(classLoader);

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


    /**
     * find item bean
     *
     * @param classLoader
     */
    private void initItemClass(final ClassLoader classLoader) {
        try {
            final Class homeUIClazz = XposedHelpers.findClass("com.tencent.mm.ui.HomeUI", classLoader);
            final Class launcherUIClazz = XposedHelpers.findClass("com.tencent.mm.ui.LauncherUI", classLoader);

            XposedHelpers.findAndHookMethod(launcherUIClazz, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object homeUIObject = null;
                    Field[] launcherUIFields = launcherUIClazz.getFields();
                    for (Field field : launcherUIFields) {
                        if (field.getType() == homeUIClazz) {
                            homeUIObject = XposedHelpers.getObjectField(param.thisObject, field.getName());
                        }
                    }
                    Field[] homeUIFields = homeUIClazz.getDeclaredFields();
                    for (Field homeUIField : homeUIFields) {
                        Object object = XposedHelpers.getObjectField(homeUIObject, homeUIField.getName());
                        if (object instanceof AdapterView.OnItemClickListener
                                && object instanceof PopupWindow.OnDismissListener) {

                            Class[] classes = object.getClass().getClasses();
                            for (Class aClass : classes) {

                                Field[] fields = aClass.getDeclaredFields();

                                if (fields.length == 5) {
                                    int intCount = 0;
                                    int stringCount = 0;
                                    for (Field field : fields) {
                                        intCount = intCount + (field.getType() == int.class ? 1 : 0);
                                        stringCount = stringCount + (field.getType() == String.class ? 1 : 0);
                                    }
                                    if (intCount == 3 && stringCount == 2) {
                                        initIconIds(aClass);
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

    /**
     * find icon id
     *
     * @param itemClass
     */
    private void initIconIds(final Class itemClass) {
        try {
            XposedBridge.hookAllConstructors(itemClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int arg0 = (int) param.args[0];
                    int iconId = (int) param.args[3];
                    if (arg0 == 10) {
                        iconIds[0] = iconId;
                    } else if (arg0 == 20) {
                        iconIds[1] = iconId;
                    }
                    super.beforeHookedMethod(param);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
