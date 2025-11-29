package com.example.calculatrice;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        long userId = SessionManager.getUserId(this);
        if (userId == -1L) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_REDIRECT_TO_PROFILE, true);
            startActivity(intent);
            finish();
            return;
        }

        AppDatabase db = AppDatabase.getInstance(this);
        User user = db.userDao().getById(userId);

        TextView tvUsername = findViewById(R.id.tvProfileUsername);
        TextView tvUserId = findViewById(R.id.tvProfileId);
        Button btnLogout = findViewById(R.id.btnLogout);

        if (user != null) {
            tvUsername.setText(user.username);
            tvUserId.setText("User ID: " + user.id);
        } else {
            tvUsername.setText("Unknown user");
            tvUserId.setText("");
        }

        btnLogout.setOnClickListener(v -> {
            SessionManager.clear(this);
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        });
    }
}
