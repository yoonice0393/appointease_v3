package com.example.sttherese;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;



public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);


        ImageView logo = findViewById(R.id.logoImage);
        TextView logoText = findViewById(R.id.logoText);



        // Load and start fade-in animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo.startAnimation(fadeIn);

        Animation fadeInText = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logoText.startAnimation(fadeInText);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreen.this, SignInPage.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION);

//        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
//        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
//
//        if (isLoggedIn) {
//            startActivity(new Intent(this, HomePage.class));
//            finish();
//        } else {
//            startActivity(new Intent(this, SignIn.class));
//            finish();
//        }

    }
}