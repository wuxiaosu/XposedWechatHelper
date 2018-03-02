package com.wuxiaosu.wechathelper.hook;

import android.widget.TextView;

import com.wuxiaosu.wechathelper.BuildConfig;
import com.wuxiaosu.widget.SettingLabelView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedHelpers.getObjectField;


/**
 * Created by su on 2018/2/05.
 * 零钱 hook
 */

public class MoneyHook {
    private static XSharedPreferences xsp;

    private static boolean fakeMoney;
    private static String money;

    private String mallIndexUIMethodName;
    private String mallIndexUIFiledName;

    private String walletBalanceManagerUIMethodName;
    private String walletBalanceManagerUIFiledName;

    public MoneyHook(String versionName) {
        switch (versionName) {
            //6.6.1 通过
            case "6.6.0":
                mallIndexUIMethodName = "aSR";
                mallIndexUIFiledName = "nya";

                walletBalanceManagerUIMethodName = "au";
                walletBalanceManagerUIFiledName = "rFA";
                break;
            case "6.6.1":
                mallIndexUIMethodName = "aTu";
                mallIndexUIFiledName = "nCe";

                walletBalanceManagerUIMethodName = "au";
                walletBalanceManagerUIFiledName = "rJK";
                break;
            case "6.6.2":
                mallIndexUIMethodName = "aYm";
                mallIndexUIFiledName = "olV";

                walletBalanceManagerUIMethodName = "au";
                walletBalanceManagerUIFiledName = "szP";
                break;
            case "6.6.3":
                mallIndexUIMethodName = "aYm";
                mallIndexUIFiledName = "olV";

                walletBalanceManagerUIMethodName = "au";
                walletBalanceManagerUIFiledName = "szP";
                break;
        }
    }

    private void reload() {
        xsp.reload();
        money = xsp.getString("money", "0.00");
        fakeMoney = xsp.getBoolean("fake_money", false);
    }

    public void hook(ClassLoader classLoader) {
        xsp = new XSharedPreferences(BuildConfig.APPLICATION_ID, SettingLabelView.DEFAULT_PREFERENCES_NAME);
        xsp.makeWorldReadable();
        try {
            Class clazz = XposedHelpers.findClass("com.tencent.mm.plugin.mall.ui.MallIndexUI", classLoader);
            if (clazz != null) {
                XposedHelpers.findAndHookMethod(clazz, mallIndexUIMethodName, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        reload();
                        if (fakeMoney) {
                            Object object = param.thisObject;
                            TextView textView = (TextView) getObjectField(object, mallIndexUIFiledName);
                            textView.setText("￥" + money);
                        }
                        super.afterHookedMethod(param);
                    }
                });
            }

            XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.wallet.balance.ui.WalletBalanceManagerUI", classLoader,
                    walletBalanceManagerUIMethodName, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            reload();
                            if (fakeMoney) {
                                Object object = param.thisObject;
                                TextView textView = (TextView) getObjectField(object, walletBalanceManagerUIFiledName);
                                textView.setText("￥" + money);
                            }
                            super.afterHookedMethod(param);
                        }
                    });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }
}
