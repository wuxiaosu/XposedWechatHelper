package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.text.TextUtils;

import com.wuxiaosu.wechathelper.api.Api;
import com.wuxiaosu.wechathelper.api.ApiFactory;
import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by su on 2018/1/29.
 */

public class RevokeMsgHook {

    private static Map<Long, Object> msgCacheMap = new HashMap<>();

    private static Object storageInsertObject;

    private static boolean disableRevoke;
    private ClassLoader classLoader;

    private static final String SQLITE_DATABASE_CLAZZ = "com.tencent.wcdb.database.SQLiteDatabase";

    private RevokeMsgHook() {

    }

    public static RevokeMsgHook getInstance() {
        return RevokeMsgHook.ExdeviceRankHookHolder.instance;
    }

    private static class ExdeviceRankHookHolder {
        @SuppressLint("StaticFieldLeak")
        private static final RevokeMsgHook instance = new RevokeMsgHook();
    }

    public void init(ClassLoader classLoader) {
        if (this.classLoader == null) {
            this.classLoader = classLoader;
            hook(classLoader);
        }
    }

    private void hook(ClassLoader classLoader) {
        try {
            Class clazz = XposedHelpers.findClass(SQLITE_DATABASE_CLAZZ, classLoader);
            XposedHelpers.findAndHookMethod(clazz, "updateWithOnConflict",
                    String.class, ContentValues.class, String.class, String[].class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args[0].equals("message")) {
                                ContentValues values = ((ContentValues) param.args[1]);
                                if (isRecalledMessage(values)) {
                                    reload();
                                    if (disableRevoke) {
                                        handleMessageRecall(values);
                                        param.setResult(1);
                                    }
                                }
                            }
                            super.beforeHookedMethod(param);
                        }
                    });
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
            XposedHelpers.findAndHookMethod(File.class, "delete", new XC_MethodHook() {
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
            Api api = ApiFactory.getCurrent();
            Class msgInfoStorageClazz = XposedHelpers.findClass(api.storage_MsgInfoStorage_class, classLoader);
            Class msgInfoClazz = XposedHelpers.findClass(api.storage_MsgInfo_class, classLoader);
            XposedHelpers.findAndHookMethod(msgInfoStorageClazz, api.storage_MsgInfoStorage_insert_method, msgInfoClazz, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            storageInsertObject = param.thisObject;
                            Object msg = param.args[0];
                            if (msg == null) {
                                return;
                            }
                            try {
                                long msgId = XposedHelpers.getLongField(msg, "field_msgId");
                                msgCacheMap.put(msgId, msg);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                            super.afterHookedMethod(param);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void reload() {
        disableRevoke = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "disable_revoke", "false"));
    }

    private static boolean isRecalledMessage(ContentValues values) {
        if (values == null) {
            return false;
        }
        String content = values.getAsString("content");
        if (!TextUtils.isEmpty(content)
                && values.getAsInteger("type") == 10000) {
            if (!content.contains("你撤回了一条消息") && !content.contains("You've recalled a message")) {
                return content.contains("\" 撤回了一条消息") || content.contains("has recalled a message");
            }
        }
        return false;
    }

    private static void handleMessageRecall(ContentValues contentValues) {
        long msgId = contentValues.getAsLong("msgId");
        Object msg = msgCacheMap.get(msgId);
        if (msg != null) {
            long createTime = XposedHelpers.getLongField(msg, "field_createTime");
            XposedHelpers.setIntField(msg, "field_type", contentValues.getAsInteger("type"));
            XposedHelpers.setObjectField(msg, "field_content",
                    contentValues.getAsString("content") + "(已被阻止)");
            XposedHelpers.setLongField(msg, "field_createTime", createTime + 1L);
            XposedHelpers.callMethod(storageInsertObject,
                    ApiFactory.getCurrent().storage_MsgInfoStorage_insert_method, msg, false);
        }
    }
}
