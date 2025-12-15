package com.example.calculatrice;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnBrowse = findViewById(R.id.btnBrowseEvents);

        btnLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        btnBrowse.setOnClickListener(v -> startActivity(new Intent(this, EventListActivity.class)));

        // Initialize Notifications
        NotificationHelper.createNotificationChannel(this);
        NotificationHelper.checkAndRequestPermission(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        String userId = SessionManager.getUserId(this);
        if (userId != null) {
            Intent intent = new Intent(this, EventListActivity.class);
            intent.putExtra(EventListActivity.EXTRA_MODE, EventListActivity.MODE_PUBLIC);
            intent.putExtra("userId", userId);
            startActivity(intent);
            finish();
        }
    }
}
