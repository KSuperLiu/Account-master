package com.silence.account.application;

import android.app.Application;

import com.silence.account.model.User;

import cn.bmob.v3.Bmob;
import cn.sharesdk.framework.ShareSDK;


public class AccountApplication extends Application {
    private String APPID = "92c02f7ec871dcc840ae1b2995b93508";
    private static AccountApplication sAccountApplication;
    public static User sUser;



    public static AccountApplication getApplication() {
        return sAccountApplication;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Bmob.initialize(getApplicationContext(), APPID);
        sAccountApplication = this;

    }
}
