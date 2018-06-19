package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Consumer;

/**
 * Created by su on 2018/03/19.
 * 微信运动界面
 */

public class ExdeviceRankHook {

    private ListView listView;
    private int likeIconId = 0;

    private ExdeviceRankHook() {
    }

    private ClassLoader classLoader;

    public void init(ClassLoader classLoader, String versionName) {
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
            Class menuClazz = XposedHelpers.findClass("com.tencent.mm.plugin.exdevice.ui.ExdeviceRankInfoUI$19", classLoader);

            Class contextMenuClazz = null;

            String methodName = "";
            Method[] methods = menuClazz.getMethods();
            for (Method method : methods) {
                if (method.getReturnType() == void.class && method.getParameterTypes().length == 1) {
                    //写法偷懒了 以后出问题了再说
                    methodName = method.getName();
                    contextMenuClazz = method.getParameterTypes()[0];
                    break;
                }
            }
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
            XposedBridge.hookAllMethods(menuClazz, methodName, new XC_MethodHook() {
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
                @SuppressLint("CheckResult")
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    MenuItem menuItem = (MenuItem) param.args[0];
                    if (menuItem.getItemId() == 4 && listView != null) {
                        Observable.create(new ObservableOnSubscribe<View>() {
                            @Override
                            public void subscribe(ObservableEmitter<View> emitter) {
                                ListAdapter listAdapter = listView.getAdapter();
                                Object itemObject = listAdapter.getItem(1);
                                Constructor[] constructors = itemObject.getClass().getDeclaredConstructors();

                                String itemInfoFieldName = "";
                                for (Constructor constructor : constructors) {
                                    Class[] classes = constructor.getParameterTypes();
                                    if (classes.length == 5) {
                                        if (classes[0] == int.class
                                                && classes[1] == int.class
                                                && classes[2] == String.class
                                                && classes[3] == String.class) {

                                            Field[] fields = itemObject.getClass().getDeclaredFields();

                                            for (Field field : fields) {
                                                if (field.getType() == classes[4]) {
                                                    itemInfoFieldName = field.getName();
                                                    break;
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }

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
                                            // 没赞过的
                                            View view = ((RelativeLayout) ((LinearLayout)
                                                    ((RelativeLayout) listAdapter.getView(i, null, null))
                                                            .getChildAt(1))
                                                    .getChildAt(1)).getChildAt(1);

                                            emitter.onNext(view);
                                        }
                                    }
                                }
                            }
                        }).subscribe(new Consumer<View>() {
                            @Override
                            public void accept(View view) {
                                view.callOnClick();
                                view.destroyDrawingCache();
                            }
                        });
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
