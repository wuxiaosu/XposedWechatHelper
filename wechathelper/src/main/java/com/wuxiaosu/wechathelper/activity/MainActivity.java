package com.wuxiaosu.wechathelper.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.wuxiaosu.wechathelper.BuildConfig;
import com.wuxiaosu.wechathelper.R;
import com.wuxiaosu.wechathelper.base.BaseActivity;
import com.wuxiaosu.widget.SettingLabelView;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends BaseActivity {

    @BindView(R.id.et_latitude)
    EditText etLatitude;
    @BindView(R.id.et_longitude)
    EditText etLongitude;
    @BindView(R.id.iv_location)
    ImageView ivLocation;
    @BindView(R.id.et_money)
    EditText etMoney;
    @BindView(R.id.slv_hide_icon)
    SettingLabelView slvHideIcon;
    @BindView(R.id.rl_morra)
    RadioGroup rlMorra;
    @BindView(R.id.rl_dice)
    RadioGroup rlDice;
    @BindView(R.id.rl_step)
    RadioGroup rlStep;

    private final String[] wechatSupportVersions =
            new String[]{"6.6.0", "6.6.1", "6.6.2", "6.6.3", "6.6.5"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getToolbar().setNavigationIcon(null);

        if (!isModuleActive()) {
            Toast.makeText(this, "模块未激活", Toast.LENGTH_SHORT).show();
        }
        initView();
    }

    private void initView() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(SettingLabelView.DEFAULT_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);

        bindPreferences(etLatitude, sharedPreferences, R.string.pre_key_latitude, null);
        bindPreferences(etLongitude, sharedPreferences, R.string.pre_key_longitude, null);
        bindPreferences(etMoney, sharedPreferences, R.string.pre_key_money, "0.00");
        bindPreferences(rlDice, sharedPreferences, R.string.pre_key_dice, "0");
        bindPreferences(rlMorra, sharedPreferences, R.string.pre_key_morra, "0");
        bindPreferences(rlStep, sharedPreferences, R.string.pre_key_step, "2");

        slvHideIcon.setCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                hideLauncherIcon(isChecked);
            }
        });
    }

    @OnClick({R.id.iv_location})
    public void onClickListener() {
        TencentMapActivity.actionStart(this,
                etLatitude.getText().toString(), etLongitude.getText().toString());
    }

    public void hideLauncherIcon(boolean isHide) {
        PackageManager packageManager = this.getPackageManager();
        int hide = isHide ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        packageManager.setComponentEnabledSetting(getAliasComponentName(), hide, PackageManager.DONT_KILL_APP);
    }

    private ComponentName getAliasComponentName() {
        return new ComponentName(MainActivity.this, "com.wuxiaosu.wechathelper.activity.MainActivity_Alias");
    }

    private void bindPreferences(View view, final SharedPreferences sharedPreferences, final int preStrResId, Object defaultValue) {
        if (view instanceof EditText) {
            String temp = sharedPreferences.getString(getString(preStrResId), (String) defaultValue);
            ((EditText) view).setText(temp);
            ((EditText) view).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    sharedPreferences.edit().putString(getString(preStrResId), s.toString()).apply();
                }
            });
            return;
        }
        if (view instanceof RadioGroup) {
            String temp = sharedPreferences.getString(getString(preStrResId), (String) defaultValue);
            if (!TextUtils.isEmpty(temp)) {
                sharedPreferences.edit().putString(getString(preStrResId), temp).apply();
                for (int i = 0; i < ((RadioGroup) view).getChildCount(); i++) {
                    if (((RadioGroup) view).getChildAt(i).getContentDescription().toString().equals(temp)) {
                        ((RadioButton) ((RadioGroup) view).getChildAt(i)).setChecked(true);
                        break;
                    }
                }
            }
            ((RadioGroup) view).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    sharedPreferences.edit().putString(getString(preStrResId), findViewById(checkedId).getContentDescription().toString()).apply();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TencentMapActivity.REQUEST_CODE && resultCode == RESULT_OK) {
            etLatitude.setText(data.getStringExtra(TencentMapActivity.LAT_KEY));
            etLongitude.setText(data.getStringExtra(TencentMapActivity.LON_KEY));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            showInfo();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetTextI18n")
    private void showInfo() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_about_content, null);
        TextView mTvVersionName = view.findViewById(R.id.tv_version_name);
        TextView mTvInfo = view.findViewById(R.id.tv_info);
        final TextView mTvUrl = view.findViewById(R.id.tv_url);
        mTvUrl.setText(Html.fromHtml("<a href=''>https://github.com/wuxiaosu/XposedWechatHelper</a>"));
        mTvUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendURLIntent(((TextView) v).getText().toString());
            }
        });
        mTvVersionName.setText(getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME);
        mTvInfo.setText(getString(R.string.app_description)
                + ",当前版本已支持\n微信："
                + Arrays.toString(wechatSupportVersions)
                + "\n更多详情：");
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("关于")
                .setView(view)
                .create();
        alertDialog.show();
    }

    private void sendURLIntent(String url) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri contentUrl = Uri.parse(url);
        intent.setData(contentUrl);
        startActivity(intent);
    }

    /**
     * 模块是否启用
     *
     * @return
     */
    private static boolean isModuleActive() {
        Log.i(MainActivity.class.getSimpleName(), "dummy log for hook : " + MainActivity.class);
        return false;
    }
}
