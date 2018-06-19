package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;
import android.content.ContentValues;

import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by su on 2018/1/29.
 */

public class RevokeMsgHook {

    private static Map<Long, Object> msgCacheMap = new HashMap<>();
    private static Object storageInsertObject;

    private static String insertMethodName;

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
            Class pluginMessengerFoundationClazz = XposedHelpers.findClass("com.tencent.mm.plugin.messenger.foundation.PluginMessengerFoundation", classLoader);
            Field[] fields = pluginMessengerFoundationClazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().getName().startsWith("com.tencent.mm.plugin.messenger.foundation")) {
                    //全凭感觉 下次要改再说
                    Field[] fieldsFields = field.getType().getDeclaredFields();
                    if (fieldsFields.length == 12) {
                        for (Field fieldsField : fieldsFields) {
                            Constructor[] constructors = fieldsField.getType().getConstructors();
                            if (constructors.length == 1 && constructors[0].getParameterTypes().length == 3) {
                                Class insertClass = fieldsField.getType();

                                Method[] methods = insertClass.getDeclaredMethods();
                                for (Method method : methods) {
                                    if (method.getParameterTypes().length == 2
                                            && method.getParameterTypes()[1] == boolean.class
                                            && method.getReturnType() == long.class) {
                                        insertMethodName = method.getName();

                                        XposedBridge.hookAllMethods(insertClass, insertMethodName,
                                                new XC_MethodHook() {
                                                    @Override
                                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                                        storageInsertObject = param.thisObject;
                                                        if (param.args.length == 2) {
                                                            Object msg = param.args[0];
                                                            long msgId = -1;
                                                            try {
                                                                msgId = XposedHelpers.getLongField(msg, "field_msgId");
                                                            } catch (Throwable e) {
                                                                e.printStackTrace();
                                                            }
                                                            msgCacheMap.put(msgId, msg);
                                                        }
                                                        super.afterHookedMethod(param);
                                                    }
                                                });

                                        break;
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    }
                }
            }
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
        XposedHelpers.callMethod(storageInsertObject, insertMethodName, msg, false);
    }
}
