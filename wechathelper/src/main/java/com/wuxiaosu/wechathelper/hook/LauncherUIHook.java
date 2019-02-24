package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.PopupWindow;

import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

/**
 * Created by su on 2018/04/12.
 * 首页
 */

public class LauncherUIHook {

    private int[] iconIds = new int[2];
    private int menuColor = 0;
    private Class mainTabUIClazz;

    private LauncherUIHook() {
    }

    private ClassLoader classLoader;

    public void init(ClassLoader classLoader, String versionName) {
        switch (versionName) {
            case "6.6.0":
            case "6.6.1":
            case "6.6.2":
            case "6.6.3":
            case "6.6.5":
            case "6.6.6":
            case "6.6.7":
            case "6.7.2":
            case "6.7.3":
                break;
            default:
            case "7.0.0":
            case "7.0.3":
                menuColor = -16777216;
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

        boolean fakeLauncherMenu = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "fake_launcher_menu", "false"));
        boolean onlyShowChat = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "only_show_chat", "false"));

        Class launcherUIClazz = XposedHelpers.findClass("com.tencent.mm.ui.LauncherUI", classLoader);

        if (fakeLauncherMenu) {

            initItemClass(classLoader);

            try {
                XposedHelpers.findAndHookMethod(launcherUIClazz, "onCreateOptionsMenu",
                        Menu.class, new XC_MethodHook() {
                            @SuppressLint("ResourceType")
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                Menu menu = (Menu) param.args[0];
                                MenuItem menuItem = menu.add(0, 3, 0, "扫一扫");
                                menuItem.setIcon(iconIds[0]);
                                setMenuItemColorFilter(menuItem);
                                menuItem.setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS);
                                menuItem = menu.add(0, 4, 0, "收付款");
                                menuItem.setIcon(iconIds[1]);
                                menuItem.setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS);
                                setMenuItemColorFilter(menuItem);
                                menu.removeItem(2);
                                super.afterHookedMethod(param);
                            }
                        });

                XposedHelpers.findAndHookMethod(launcherUIClazz, "onOptionsItemSelected",
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

        if (onlyShowChat) {
            try {
                try {
                    mainTabUIClazz = XposedHelpers.findClass("com.tencent.mm.ui.MainTabUI", classLoader);
                    Class adapterClass = XposedHelpers.findClass("com.tencent.mm.ui.MainTabUI$TabsAdapter", classLoader);
                    hookTabsAdapter(adapterClass);
                } catch (Throwable e) {
                    Class homeUIClazz = XposedHelpers.findClass("com.tencent.mm.ui.HomeUI", classLoader);
                    Field[] fields = homeUIClazz.getFields();
                    for (Field field : fields) {
                        if (field.getType().getName().startsWith("com.tencent.mm.ui")) {
                            mainTabUIClazz = field.getType();

                            hookTabsAdapter(mainTabUIClazz.getDeclaredClasses()[0]);
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }


            try {
                Class conversationListViewClazz = XposedHelpers.findClass("com.tencent.mm.ui.conversation.ConversationListView", classLoader);

                XposedHelpers.findAndHookMethod(conversationListViewClazz, "init", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);

                        Object object = param.thisObject;
                        for (Field declaredField : object.getClass().getDeclaredFields()) {
                            if (declaredField.getType().getName().startsWith("com.tencent.mm.plugin.appbrand.widget.header")) {
                                Object controllerObject = XposedHelpers.getObjectField(object, declaredField.getName());
                                if (controllerObject != null) {
                                    for (Method method : controllerObject.getClass().getMethods()) {
                                        if (method.getName().equals("setTabView")) {
                                            XposedHelpers.findAndHookMethod(controllerObject.getClass(), "setTabView", View.class, new XC_MethodHook() {
                                                @Override
                                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                                    param.args[0] = null;
                                                    super.beforeHookedMethod(param);
                                                }
                                            });
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
            } catch (Exception ignored) {
            }

            try {
                XposedHelpers.findAndHookMethod(launcherUIClazz, "onResume", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        Object thisObject = param.thisObject;
                        Object MainTabUIObject = null;
                        Object HomeUIObject = findObject(thisObject, thisObject.getClass().getDeclaredFields(), "com.tencent.mm.ui.HomeUI");
                        if (HomeUIObject != null && mainTabUIClazz != null) {
                            MainTabUIObject = findObject(HomeUIObject, HomeUIObject.getClass().getDeclaredFields(), mainTabUIClazz.getName());
                        }
                        if (MainTabUIObject != null) {
                            for (Field declaredField : MainTabUIObject.getClass().getFields()) {
                                if (declaredField.getType().getName().startsWith("com.tencent.mm.ui")) {
                                    Object object = XposedHelpers.getObjectField(MainTabUIObject, declaredField.getName());
                                    for (Field field : object.getClass().getDeclaredFields()) {
                                        String typeName = field.getType().getName();
                                        if (typeName.startsWith("com.tencent.mm.ui") && !typeName.contains("$") && !typeName.endsWith("Activity")) {
                                            View LauncherUIBottomTabView = (View) XposedHelpers.getObjectField(object, field.getName());
                                            if (LauncherUIBottomTabView != null) {
                                                LauncherUIBottomTabView.setVisibility(View.GONE);
                                            }
                                            break;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        super.afterHookedMethod(param);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setMenuItemColorFilter(MenuItem menuItem) {
        Drawable drawable = menuItem.getIcon();
        if (drawable != null && menuColor != 0) {
            drawable.mutate();
            drawable.setColorFilter(menuColor, PorterDuff.Mode.SRC_ATOP);
        }
    }

    private Object findObject(Object object, Field[] fields, String className) {
        if (object == null) {
            return null;
        }
        for (Field field : fields) {
            if (field.getType().getName().equals(className)) {
                return XposedHelpers.getObjectField(object, field.getName());
            }
        }
        return null;
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

    private void hookTabsAdapter(Class adapterClass) {
        XposedHelpers.findAndHookMethod(adapterClass, "getCount", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return 1;
            }
        });

        for (Method declaredMethod : adapterClass.getDeclaredMethods()) {
            Class[] parameterTypes = declaredMethod.getParameterTypes();
            if (parameterTypes.length == 2 &&
                    parameterTypes[0] == int.class
                    && parameterTypes[1] == int.class
                    && declaredMethod.getReturnType() == void.class) {

                XposedHelpers.findAndHookMethod(adapterClass, declaredMethod.getName(), int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        param.args[0] = 0;
                    }
                });
                break;
            }
        }
    }
}
