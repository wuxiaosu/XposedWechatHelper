package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;
import android.content.ContentValues;

import com.wuxiaosu.wechathelper.BuildConfig;
import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.SettingLabelView;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by su on 2018/1/29.
 */

public class RevokeMsgHook {

    private static Map<Long, Object> msgCacheMap = new HashMap<>();
    private static Object storageInsertClazz;

    private String insertClassName;
    private String insertMethodName;

    private static boolean disableRevoke;
    private ClassLoader classLoader;

    private RevokeMsgHook() {

    }

    public static RevokeMsgHook getInstance() {
        return RevokeMsgHook.ExdeviceRankHookHolder.instance;
    }

    private static class ExdeviceRankHookHolder {
        @SuppressLint("StaticFieldLeak")
        private static final RevokeMsgHook instance = new RevokeMsgHook();
    }

    public void init(ClassLoader classLoader, String versionName) {
        insertClassName = "com.tencent.mm.storage.av";
        insertMethodName = "b";
        if (versionName.startsWith("6.6.6")) {
            insertClassName = "com.tencent.mm.storage.ba";
            insertMethodName = "b";
        }
        if (this.classLoader == null) {
            this.classLoader = classLoader;
            hook(classLoader);
        }
    }

    private void hook(ClassLoader classLoader) {
        try {
            Class clazz = XposedHelpers.findClass("com.tencent.wcdb.database.SQLiteDatabase", classLoader);
            XposedHelpers.findAndHookMethod(clazz, "updateWithOnConflict",
                    String.class, ContentValues.class, String.class, String[].class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args[0].equals("message")) {
                                ContentValues contentValues = ((ContentValues) param.args[1]);
                                reload();

                                if (disableRevoke && contentValues.getAsInteger("type") == 10000 &&
                                        !contentValues.getAsString("content").equals("你撤回了一条消息")) {
                                    handleMessageRecall(contentValues);
                                    param.setResult(1);
                                }
                            }

                            super.beforeHookedMethod(param);
                        }
                    });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }

        try {
            Class clazz = XposedHelpers.findClass("com.tencent.wcdb.database.SQLiteDatabase", classLoader);
            XposedHelpers.findAndHookMethod(clazz, "delete",
                    String.class, String.class, String[].class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String[] media = {"ImgInfo2", "voiceinfo", "videoinfo2", "WxFileIndex2"};
                            if (disableRevoke && Arrays.asList(media).contains(param.args[0])) {
                                param.setResult(1);
                            }
                            super.beforeHookedMethod(param);
                        }
                    });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }

        try {
            XposedHelpers.findAndHookMethod(File.class, "delete",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String path = ((File) param.thisObject).getAbsolutePath();
                            if (disableRevoke &&
                                    (path.contains("/image2/") || path.contains("/voice2/") || path.contains("/video/")))
                                param.setResult(true);
                            super.beforeHookedMethod(param);
                        }
                    });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }

        try {
            // insert method
            Class clazz = XposedHelpers.findClass(insertClassName, classLoader);
            XposedBridge.hookAllMethods(clazz, insertMethodName,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            storageInsertClazz = param.thisObject;
                            Object msg = param.args[0];
                            long msgId = -1;
                            try {
                                msgId = XposedHelpers.getLongField(msg, "field_msgId");
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                            msgCacheMap.put(msgId, msg);
                            super.afterHookedMethod(param);
                        }
                    });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }

    private static void reload() {
        disableRevoke = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "disable_revoke", "false"));
    }

    private static void handleMessageRecall(ContentValues contentValues) {
        long msgId = contentValues.getAsLong("msgId");
        Object msg = msgCacheMap.get(msgId);

        long createTime = XposedHelpers.getLongField(msg, "field_createTime");
        XposedHelpers.setIntField(msg, "field_type", contentValues.getAsInteger("type"));
        XposedHelpers.setObjectField(msg, "field_content",
                contentValues.getAsString("content") + "(已被阻止)");
        XposedHelpers.setLongField(msg, "field_createTime", createTime + 1L);
        XposedHelpers.callMethod(storageInsertClazz, "b", msg, false);
    }
}
