package com.wuxiaosu.wechathelper.hook;

import android.widget.TextView;

import com.wuxiaosu.wechathelper.BuildConfig;
import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.SettingLabelView;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedHelpers.getObjectField;


/**
 * Created by su on 2018/2/05.
 * 零钱 hook
 */

public class MoneyHook {

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
            case "6.6.5":
                mallIndexUIMethodName = "aYS";
                mallIndexUIFiledName = "orA";

                walletBalanceManagerUIMethodName = "av";
                walletBalanceManagerUIFiledName = "sFT";
                break;
            default:
            case "6.6.6":
                mallIndexUIMethodName = "bbL";
                mallIndexUIFiledName = "oFV";

                walletBalanceManagerUIMethodName = "aF";
                walletBalanceManagerUIFiledName = "ths";
                break;
        }
    }

    private void reload() {
        money = PropertiesUtils.getValue(Constant.PRO_FILE, "money", "0.00");
        fakeMoney = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "fake_money", "false"));
    }

    public void hook(ClassLoader classLoader) {
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
