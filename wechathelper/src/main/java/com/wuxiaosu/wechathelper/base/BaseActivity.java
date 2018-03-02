package com.wuxiaosu.wechathelper.base;

import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.jaeger.library.StatusBarUtil;
import com.wuxiaosu.wechathelper.R;

import butterknife.ButterKnife;

/**
 * Created by su on 2018/1/7.
 */

public class BaseActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        initStatusBar();
        initToolbar();
        ButterKnife.bind(this);
    }

    /**
     * 初始化状态栏颜色
     * 更多详情https://github.com/laobie/StatusBarUtil
     */
    protected void initStatusBar() {
        if (isStatusBarTransparent()) {
            StatusBarUtil.setTransparent(BaseActivity.this);
            return;
        }
        StatusBarUtil.setColor(this,
                (ContextCompat.getColor(this, getStatusBarColor())), 0);
    }

    /**
     * 状态栏是否透明
     *
     * @return 默认否
     */
    protected boolean isStatusBarTransparent() {
        return false;
    }

    /**
     * 状态栏默认颜色,不支持动态修改
     *
     * @return 状态栏颜色
     */
    @ColorRes
    protected int getStatusBarColor() {
        return R.color.status_bar;
    }


    private void initToolbar() {
        mToolbar = findViewById(R.id.toolbar);
        if (null == mToolbar) {
            return;
        }
        ActivityInfo mActivityInfo = null;
        try {
            mActivityInfo = getPackageManager().
                    getActivityInfo(this.getComponentName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        // 在 AndroidManifest 中添加 android:label 设置默认标题栏
        if (mActivityInfo != null && 0 != mActivityInfo.labelRes) {
            mToolbar.setTitle(mActivityInfo.labelRes);
        } else {
            mToolbar.setTitle("");
        }
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    /**
     * 显示状态栏
     */
    public void showToolbar() {
        getToolbar().setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏状态栏
     */
    public void hideToolbar() {
        getToolbar().setVisibility(View.GONE);
    }

    private ProgressDialog mProgressDialog;

    /**
     * 显示 progress dialog
     */
    public void showProgressDialog() {
        showProgressDialog("请稍候...", false);
    }

    /**
     * 显示 progress dialog
     *
     * @param cancelable 点击空白处是否可以关闭
     */
    public void showProgressDialog(boolean cancelable) {
        showProgressDialog("请稍候...", cancelable);
    }

    /**
     * 显示 progress dialog
     *
     * @param text
     * @param cancelable
     */
    public void showProgressDialog(String text, boolean cancelable) {
        if (null == mProgressDialog) {
            mProgressDialog = new ProgressDialog(this);
        }
        mProgressDialog.setMessage(text);
        mProgressDialog.setCancelable(cancelable);
        mProgressDialog.show();
    }

    /**
     * 关闭 progress dialog
     */
    public void dismissProgressDialog() {
        if (null != mProgressDialog && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /***
     * 双击退出
     *
     * @param text
     */
    protected void exitBy2Click(String text) {
        long secondTime = System.currentTimeMillis();
        if (secondTime - firstTime > 2000) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            firstTime = secondTime;
        } else {
            finish();
        }
    }

    private long firstTime = 0;
}
