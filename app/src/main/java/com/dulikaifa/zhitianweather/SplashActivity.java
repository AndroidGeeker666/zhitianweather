package com.dulikaifa.zhitianweather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

public class SplashActivity extends Activity {
    private final int SPLASH_DISPLAY_LENGHT = 2000; // 延迟两秒

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SplashActivity.this);
                if (prefs.getString("weather", null) != null) {
                    Intent intent = new Intent(SplashActivity.this, WeatherActivity.class);
                    SplashActivity.this.startActivity(intent);
                    SplashActivity.this.finish();
                }else{
                    Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                    SplashActivity.this.startActivity(mainIntent);//显示主窗口
                    SplashActivity.this.finish();//关闭主窗口
                }
            }
        }, SPLASH_DISPLAY_LENGHT);
    }
}