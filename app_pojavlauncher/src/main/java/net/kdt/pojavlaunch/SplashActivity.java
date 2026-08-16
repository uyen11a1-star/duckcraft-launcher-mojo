package net.kdt.pojavlaunch;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import git.artdeell.mojo.R;

public class SplashActivity extends AppCompatActivity {

    // Keep a brief transition without imposing a noticeable startup delay.
    private static final long SPLASH_DURATION_MS = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, TestStorageActivity.class));
            finish();
        }, SPLASH_DURATION_MS);
    }
}
