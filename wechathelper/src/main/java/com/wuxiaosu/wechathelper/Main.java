package com.wuxiaosu.wechathelper;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.wuxiaosu.wechathelper.hook.EmojiGameHook;
import com.wuxiaosu.wechathelper.hook.MoneyHook;
import com.wuxiaosu.wechathelper.hook.RevokeMsgHook;
import com.wuxiaosu.wechathelper.hook.StepHook;
import com.wuxiaosu.wechathelper.hook.TencentLocationManagerHook;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by su on 2017/12/29.
 */

public class Main implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (lpparam.appInfo == null || (lpparam.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM |
                ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
            return;
        }

        if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
            XposedHelpers.findAndHookMethod("com.wuxiaosu.wechathelper.activity.MainActivity", lpparam.classLoader,
                    "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        if (lpparam.packageName.equals("com.tencent.mm")) {
            try {
                XposedHelpers.findAndHookMethod(Application.class,
                        "attach",
                        Context.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                Context context = (Context) param.args[0];
                                ClassLoader appClassLoader = context.getClassLoader();
                                handleHook(appClassLoader,
                                        getVersionName(context, "com.tencent.mm"));
                            }
                        });
            } catch (Error | Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleHook(ClassLoader classLoader, String versionName) {
        new TencentLocationManagerHook(versionName).hook(classLoader);
        new EmojiGameHook(versionName).hook(classLoader);
        new MoneyHook(versionName).hook(classLoader);
        StepHook.hook(classLoader);
        RevokeMsgHook.hook(classLoader);
    }

    private String getVersionName(Context context, String pkgName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(pkgName, 0);
            return packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

}
