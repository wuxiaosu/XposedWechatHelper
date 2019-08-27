package com.wuxiaosu.wechathelper.api;

/**
 * Created by su on 2019/8/27.
 */

public class ApiFactory {

    private static Api sCurrent = null;

    public static Api getCurrent() {
        return sCurrent;
    }

    public static void initApi(String versionName) {
        switch (versionName) {
            case Api703.SUPPORT_VERSION_NAME:
                sCurrent = new Api703();
                break;
            case Api704.SUPPORT_VERSION_NAME:
                sCurrent = new Api704();
                break;
            case Api705.SUPPORT_VERSION_NAME:
                sCurrent = new Api705();
                break;
            default:
            case Api706.SUPPORT_VERSION_NAME:
                sCurrent = new Api706();
                break;
        }
    }
}
