package com.example.ws;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.ws.bean.Config;
import com.example.ws.bean.User;
import com.example.ws.picker.DatePickerDialog;
import com.example.ws.picker.HeightPickerDialog;
import com.example.ws.util.DateUtils;
import com.example.ws.util.ToastMaker;
import com.yolanda.health.qnblesdk.constant.QNInfoConst;
import com.yolanda.health.qnblesdk.constant.UserShape;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UserInfoActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    @BindView(R.id.bodyShapeSpinner)
    Spinner bodyShapeSpn;

    private TextView heightTv;

    @BindView(R.id.birthday_view)
    TextView birthdayTv;

    @BindView(R.id.user_gender_grp)
    RadioGroup userGenderGroup;
    @BindView(R.id.male_rb)
    RadioButton maleRb;
    @BindView(R.id.female_rb)
    RadioButton femaleRb;

    @BindView(R.id.user_info_confirm_btn)
    Button userInfoConfirmBtn;

    private int mHeight = 172;
    private Date birthday = null;
    private String gender = "male";
    private User user;
    private Config config;

    public static Intent getCallIntent(Context context) {
        return new Intent(context, UserInfoActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        ButterKnife.bind(this);
        heightTv = (TextView) findViewById(R.id.heightTv);
        initData();
        initListener();
        initView();
    }

    private void initData () {
        user = new User();
        config = new Config();

        // Set a dummy user ID
        // TODO: Make it dynamic + add other values
        user.setUserId("1234");
        user.setHeight(mHeight);
        user.setGender(gender);
    }

    private void initListener () {
        heightTv.setOnClickListener(this);
        birthdayTv.setOnClickListener(this);
        userGenderGroup.setOnCheckedChangeListener(this);
        userInfoConfirmBtn.setOnClickListener(this);

        bodyShapeSpn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                user.setChoseShape(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                user.setChoseShape(UserShape.SHAPE_NONE.getCode());
            }
        });
    }

    private void initView() {
        heightTv.setText(mHeight + "cm");
        user.setHeight(mHeight);
        birthdayTv.setText("Click here to select");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.heightTv:
                // Show Picker

                HeightPickerDialog.Builder heightBuilder = new HeightPickerDialog.Builder().heightChooseListener(new HeightPickerDialog.HeightChooseListener() {
                    @Override
                    public void onChoose(int height) {
                        mHeight = height;
                        heightTv.setText(height + "cm");
                        user.setHeight(mHeight);
                    }
                });

                heightBuilder.defaultHeight(172);
                heightBuilder.maxHeight(240);
                heightBuilder.minHeight(40);
                heightBuilder.themeColor(getResources().getColor(R.color.themeColor_0fbfef)).context(UserInfoActivity.this).build().show();
                break;
            case R.id.birthday_view:
                DatePickerDialog.Builder dateBuilder = new DatePickerDialog.Builder()
                        .dateChooseListener(new DatePickerDialog.DateChooseListener() {

                            @Override
                            public void onChoose(Date date) {
                                if (DateUtils.getAge(date) < 10) {
                                    ToastMaker.show(UserInfoActivity.this, getResources().getString(R.string.RegisterViewController_lowAge10));
                                }

                                birthday = date;
                                birthdayTv.setText(DateUtils.getBirthdayString(date, UserInfoActivity.this));
                                user.setBirthDay(birthday);
                            }
                        });

                dateBuilder.defaultYear(1990);
                dateBuilder.defaultMonth(1);
                dateBuilder.defaultDay(1);
                dateBuilder.context(UserInfoActivity.this)
                        .themeColor(getResources().getColor(R.color.themeColor_0fbfef))
                        .build().show();
                break;
            case R.id.user_info_confirm_btn:
                startActivity(DeviceConnectActivity.getCallIntent(this, user, config));
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.male_rb:
                gender = QNInfoConst.GENDER_MAN;
                user.setGender(gender);
                break;
            case R.id.female_rb:
                gender = QNInfoConst.GENDER_WOMAN;
                user.setGender(gender);
                break;
        }
    }
}
