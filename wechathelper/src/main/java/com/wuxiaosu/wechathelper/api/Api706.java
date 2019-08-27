package com.wuxiaosu.wechathelper.api;

/**
 * Created by su on 2019/8/27.
 */

public class Api706 extends Api {

    public static final String SUPPORT_VERSION_NAME = "7.0.6";

    Api706() {
        this.storage_MsgInfoStorage_class = "com.tencent.mm.storage.bj";
        this.storage_MsgInfoStorage_insert_method = "c";
        this.storage_MsgInfo_class = "com.tencent.mm.storage.bi";
    }

    @Override
    public String getSupportVersionName() {
        return SUPPORT_VERSION_NAME;
    }
}
