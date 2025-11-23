package com.example.calculatrice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    TextView tvUsername;
    Button btnLogout;
    AppDatabase db;
    long userId;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = AppDatabase.getInstance(this);

        userId = getSharedPreferences("auth", MODE_PRIVATE).getLong("userId", -1);
        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        user = db.userDao().getById(userId); // we will add this DAO method
        tvUsername = findViewById(R.id.tvProfileUsername);
        btnLogout = findViewById(R.id.btnLogout);

        if (user != null) tvUsername.setText(user.username);

        btnLogout.setOnClickListener(v -> {
            SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
            sp.edit().remove("userId").apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
