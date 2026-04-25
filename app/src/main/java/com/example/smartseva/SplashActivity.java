package com.example.smartseva;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    ProgressBar splashProgress;
    TextView tvLoadingMsg;
    Handler handler = new Handler();
    int progress = 0;

    String[] messages = {
            "Loading Smart Seva...",
            "Connecting volunteers...",
            "Fetching community needs...",
            "Initializing AI matching...",
            "Almost ready!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);

        splashProgress = findViewById(R.id.splashProgress);
        tvLoadingMsg   = findViewById(R.id.tvLoadingMsg);

        animateProgress();
    }

    void animateProgress() {
        handler.postDelayed(new Runnable() {
            int msgIndex = 0;

            @Override
            public void run() {
                progress += 20;
                splashProgress.setProgress(progress);

                if (msgIndex < messages.length) {
                    tvLoadingMsg.setText(messages[msgIndex++]);
                }

                if (progress < 100) {
                    handler.postDelayed(this, 400);
                } else {
                    // Go to MainActivity
                    handler.postDelayed(() -> {
                        startActivity(new Intent(
                                SplashActivity.this, MainActivity.class));
                        overridePendingTransition(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out);
                        finish();
                    }, 300);
                }
            }
        }, 400);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}