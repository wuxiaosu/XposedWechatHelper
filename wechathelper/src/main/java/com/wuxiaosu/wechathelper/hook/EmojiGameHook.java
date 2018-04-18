package com.wuxiaosu.wechathelper.hook;

import com.wuxiaosu.wechathelper.BuildConfig;
import com.wuxiaosu.widget.SettingLabelView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by su on 2018/02/06.
 * 骰子 猜拳 hook
 */

public class EmojiGameHook {
    private XSharedPreferences xsp;

    private boolean fakeMorra;
    private String morra;
    private boolean fakeDice;
    private String dice;
    private String methodName;
    private String clazzName;

    public EmojiGameHook(String versionName) {
        switch (versionName) {
            case "6.6.0":
                clazzName = "com.tencent.mm.sdk.platformtools.bh";
                methodName = "em";
                break;
            case "6.6.1":
                clazzName = "com.tencent.mm.sdk.platformtools.bh";
                methodName = "en";
                break;
            case "6.6.2":
                clazzName = "com.tencent.mm.sdk.platformtools.bh";
                methodName = "eF";
                break;
            case "6.6.3":
                clazzName = "com.tencent.mm.sdk.platformtools.bh";
                methodName = "eF";
                break;
            case "6.6.5":
                clazzName = "com.tencent.mm.sdk.platformtools.bi";
                methodName = "eI";
                break;
            default:
            case "6.6.6":
                clazzName = "com.tencent.mm.sdk.platformtools.bh";
                methodName = "eE";
                break;
        }
    }

    public void hook(ClassLoader classLoader) {
        xsp = new XSharedPreferences(BuildConfig.APPLICATION_ID, SettingLabelView.DEFAULT_PREFERENCES_NAME);
        xsp.makeWorldReadable();
        try {
            Class clazz = XposedHelpers.findClass(clazzName, classLoader);
            XposedHelpers.findAndHookMethod(clazz, methodName, int.class, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int result = (int) param.getResult();
                    int type = (int) param.args[0];
                    reload();
                    switch (type) {
                        case 5:
                            if (fakeDice) {
                                result = Integer.valueOf(dice);
                            }
                            break;
                        case 2:
                            if (fakeMorra) {
                                result = Integer.valueOf(morra);
                            }
                            break;
                    }
                    param.setResult(result);
                    super.afterHookedMethod(param);
                }
            });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }

    private void reload() {
        xsp.reload();
        fakeMorra = xsp.getBoolean("fake_morra", false);
        fakeDice = xsp.getBoolean("fake_dice", false);
        morra = xsp.getString("morra", "0");
        dice = xsp.getString("dice", "0");
    }
}
