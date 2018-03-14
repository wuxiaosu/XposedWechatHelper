package com.wuxiaosu.wechathelper.hook;

import android.content.ContentValues;
import android.util.Log;

import com.wuxiaosu.wechathelper.BuildConfig;
import com.wuxiaosu.widget.SettingLabelView;

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
    private static XSharedPreferences xsp;

    private static boolean disableRevoke;

    // TODO: 2018/3/14 语音消息撤回后无法播放

    public static void hook(ClassLoader classLoader) {
        xsp = new XSharedPreferences(BuildConfig.APPLICATION_ID, SettingLabelView.DEFAULT_PREFERENCES_NAME);
        xsp.makeWorldReadable();
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
            // insert method
            Class clazz = XposedHelpers.findClass("com.tencent.mm.storage.av", classLoader);
            XposedBridge.hookAllMethods(clazz, "b",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            storageInsertClazz = param.thisObject;
                            Object msg = param.args[0];
                            long msgId = XposedHelpers.getLongField(msg, "field_msgId");
                            msgCacheMap.put(msgId, msg);
                            super.afterHookedMethod(param);
                        }
                    });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }


    private static void reload() {
        xsp.reload();
        disableRevoke = xsp.getBoolean("disable_revoke", false);
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
