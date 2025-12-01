package com.example.calculatrice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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
        EditText etNewUsername = findViewById(R.id.etNewUsername);
        EditText etNewPassword = findViewById(R.id.etNewPassword);
        EditText etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        Button btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        Button btnLogout = findViewById(R.id.btnLogout);

        if (user != null) {
            tvUsername.setText(user.username);
            tvUserId.setText("User ID: " + user.id);
        } else {
            tvUsername.setText("Unknown user");
            tvUserId.setText("");
        }

        btnUpdateProfile.setOnClickListener(v -> {
            if (user == null) {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String newUsername = etNewUsername.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmNewPassword.getText().toString().trim();

            if (TextUtils.isEmpty(newUsername) && TextUtils.isEmpty(newPassword)) {
                Toast.makeText(this, "Nothing to update", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate and update username
            if (!TextUtils.isEmpty(newUsername)) {
                if (newUsername.length() < 4) {
                    Toast.makeText(this, "Username must be at least 4 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                User existing = db.userDao().findByUsername(newUsername);
                if (existing != null && existing.id != user.id) {
                    Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show();
                    return;
                }
                user.username = newUsername;
            }

            // Validate and update password
            if (!TextUtils.isEmpty(newPassword)) {
                if (newPassword.length() < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }
                user.password = newPassword;
            }

            db.userDao().update(user);
            SessionManager.saveUser(this, user);

            tvUsername.setText(user.username);
            etNewUsername.setText("");
            etNewPassword.setText("");
            etConfirmNewPassword.setText("");

            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            SessionManager.clear(this);
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        });

        setupBottomNav();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                long loggedId = SessionManager.getUserId(this);
                if (loggedId == -1L) {
                    startActivity(new Intent(this, MainActivity.class));
                } else {
                    startActivity(new Intent(this, DashboardActivity.class)
                            .putExtra("userId", loggedId));
                }
                return true;
            } else if (id == R.id.nav_events) {
                startActivity(new Intent(this, EventListActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                // already here
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
