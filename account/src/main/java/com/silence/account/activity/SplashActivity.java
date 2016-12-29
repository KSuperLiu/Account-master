package com.silence.account.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;

import com.silence.account.R;
import com.silence.account.utils.Constant;

import cn.bmob.v3.BmobUser;

public class SplashActivity extends Activity {
    private LinearLayout ll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ll = (LinearLayout) findViewById(R.id.linearTestScale);
        final Intent intent = new Intent();
        if (BmobUser.getCurrentUser(this) != null) {
            //当前用户登录过，系统有缓存，设置跳转页面为主界面
            intent.setClass(this, MainActivity.class);
        } else {
            //当前用户未登录，设置跳转页面为登录页
            intent.setClass(this, LoginActivity.class);
        }
        //延迟一秒钟加载新的窗口
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //开启新的界面
                startActivity(intent);
                //添加渐变的过渡动画
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                //结束当前启动页
                finish();
            }
        }, Constant.DELAY_TIME);
    }

    private boolean hasAnimationStarted;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !hasAnimationStarted) {
            ObjectAnimator revealAnimator = ObjectAnimator.ofFloat( //缩放X 轴的
                    ll, "scaleX", 0, 200);
            ObjectAnimator revealAnimator1 = ObjectAnimator.ofFloat(//缩放Y 轴的
                    ll, "scaleY", 0, 200);
            AnimatorSet set = new AnimatorSet();
            set.setDuration(3000);//设置播放时间
            set.setInterpolator(new LinearInterpolator());//设置播放模式，这里是平常模式
            set.playTogether(revealAnimator, revealAnimator1);//设置一起播放
            set.start();
            hasAnimationStarted = true;
        }
    }

}