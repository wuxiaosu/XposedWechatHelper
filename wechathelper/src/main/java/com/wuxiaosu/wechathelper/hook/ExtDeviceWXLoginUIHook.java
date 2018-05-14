package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;
import android.widget.Button;

import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;


/**
 * Created by su on 2018/05/13.
 * 自动登录
 */
public class ExtDeviceWXLoginUIHook {

    private boolean pcAutoLogin;

    private ExtDeviceWXLoginUIHook() {
    }

    private ClassLoader classLoader;

    public void init(ClassLoader classLoader, String versionName) {
        if (this.classLoader == null) {
            this.classLoader = classLoader;
            hook(classLoader);
        }
    }

    public static ExtDeviceWXLoginUIHook getInstance() {
        return ExtDeviceWXLoginUIHook.SingletonHolder.instance;
    }

    private static class SingletonHolder {
        @SuppressLint("StaticFieldLeak")
        private static final ExtDeviceWXLoginUIHook instance = new ExtDeviceWXLoginUIHook();
    }

    private void hook(final ClassLoader classLoader) {
        pcAutoLogin = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "pc_auto_login", "false"));

        if (!pcAutoLogin) {
            return;
        }

        try {
            final Class extDeviceWXLoginUIHookClazz = XposedHelpers.findClass("com.tencent.mm.plugin.webwx.ui.ExtDeviceWXLoginUI", classLoader);
            XposedHelpers.findAndHookMethod(extDeviceWXLoginUIHookClazz, "initView",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object object = param.thisObject;
                            Field[] fields = extDeviceWXLoginUIHookClazz.getDeclaredFields();
                            for (Field field : fields) {
                                Object objectField = XposedHelpers.getObjectField(object, field.getName());
                                if (objectField instanceof Button && ((Button) objectField).getText().toString().equals("登录")) {
                                    ((Button) objectField).callOnClick();
                                    break;
                                }
                            }
                            super.afterHookedMethod(param);
                        }
                    });
        } catch (Error | Exception e) {
            XposedBridge.log(e);
        }
    }
}
