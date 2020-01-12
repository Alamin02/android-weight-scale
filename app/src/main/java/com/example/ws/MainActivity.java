package com.example.ws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.qingniu.qnble.utils.QNLogUtils;
import com.yolanda.health.qnblesdk.listener.QNResultCallback;
import com.yolanda.health.qnblesdk.out.QNBleApi;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static Intent getCallIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    private TextView username;
    private TextView email;

    private Button confirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String encryptPath = "file:///android_asset/123456789.qn";
        QNLogUtils.setLogEnable(BuildConfig.DEBUG);// Set the log print switch, which is off by default
        QNBleApi mQNBleApi = QNBleApi.getInstance(this);
        mQNBleApi.initSdk("123456789", encryptPath, new QNResultCallback() {
            @Override
            public void onResult(int code, String msg) {
                Log.d("BaseApplication", "Initialization file " + msg);
            }
        });
        setContentView(R.layout.activity_main);

        username = (TextView) findViewById(R.id.usernameInput);
        email = (TextView) findViewById(R.id.emailInput);
        confirmButton = (Button) findViewById(R.id.userInfoConfirmBtn);
        initData();
    }

    private void initData() {
        confirmButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.userInfoConfirmBtn:
                Intent intent = new Intent(this, UserInfoActivity.class);
                startActivity(intent);
                break;
        }
    }
}
