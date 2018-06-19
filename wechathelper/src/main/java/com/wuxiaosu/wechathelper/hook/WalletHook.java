package com.wuxiaosu.wechathelper.hook;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.wuxiaosu.wechathelper.utils.Constant;
import com.wuxiaosu.widget.utils.PropertiesUtils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedHelpers.getObjectField;


/**
 * Created by su on 2018/2/05.
 * 零钱 hook
 */

public class WalletHook {

    private static boolean fakeMoney;
    private static String money;

    private ClassLoader classLoader;

    private WalletHook() {

    }

    public void init(ClassLoader classLoader, String versionName) {
        if (this.classLoader == null) {
            this.classLoader = classLoader;
            hook(classLoader);
        }
    }

    public static WalletHook getInstance() {
        return WalletHook.SingletonHolder.instance;
    }

    private static class SingletonHolder {
        @SuppressLint("StaticFieldLeak")
        private static final WalletHook instance = new WalletHook();
    }

    private void reload() {
        money = PropertiesUtils.getValue(Constant.PRO_FILE, "money", "0.00");
        fakeMoney = Boolean.valueOf(PropertiesUtils.getValue(Constant.PRO_FILE, "fake_money", "false"));
    }

    private void hook(final ClassLoader classLoader) {
        final Class mallIndexUIClazz =
                XposedHelpers.findClass("com.tencent.mm.plugin.mall.ui.MallIndexUI", classLoader);
        final Class walletBalanceManagerUIClazz =
                XposedHelpers.findClass("com.tencent.mm.plugin.wallet.balance.ui.WalletBalanceManagerUI", classLoader);

        handleHook(mallIndexUIClazz, mallIndexUIClazz.getSuperclass().getDeclaredFields());
        handleHook(walletBalanceManagerUIClazz, walletBalanceManagerUIClazz.getDeclaredFields());
    }

    private void handleHook(Class clazz, final Field[] fields) {
        try {
            XposedHelpers.findAndHookMethod(clazz, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    reload();
                    if (fakeMoney) {
                        Object object = param.thisObject;
                        for (Field field : fields) {
                            if (field.getType() == TextView.class) {
                                final TextView textView = (TextView) getObjectField(object, field.getName());
                                if (textView != null) {
                                    textView.addTextChangedListener(new TextWatcher() {
                                        @Override
                                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                        }

                                        @Override
                                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                                        }

                                        @Override
                                        public void afterTextChanged(Editable s) {
                                            String string = s.toString();
                                            if (string.startsWith("¥") && !s.toString().equals("¥" + money)) {
                                                textView.setText("¥" + money);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                    super.afterHookedMethod(param);
                }
            });
        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }
}
