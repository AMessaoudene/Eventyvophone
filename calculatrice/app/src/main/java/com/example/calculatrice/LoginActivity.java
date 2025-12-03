package com.example.calculatrice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_REDIRECT_TO_PROFILE = "redirect_to_profile";

    private EditText etUsername;
    private EditText etPassword;
    private AppDatabase db;
    private boolean redirectToProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = AppDatabase.getInstance(this);
        redirectToProfile = getIntent().getBooleanExtra(EXTRA_REDIRECT_TO_PROFILE, false);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra(EXTRA_REDIRECT_TO_PROFILE, redirectToProfile);
            startActivity(intent);
        });

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> 
            startActivity(new Intent(this, ForgotPasswordActivity.class))
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        redirectToProfile = intent.getBooleanExtra(EXTRA_REDIRECT_TO_PROFILE, false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        long existingUserId = SessionManager.getUserId(this);
        if (existingUserId != -1L) {
            navigateAfterAuth(existingUserId);
            finish();
        }
    }

    private void login() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter username & password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            return;
        }

        User user = db.userDao().login(username, password);
        if (user != null) {
            SessionManager.saveUser(this, user);
            navigateAfterAuth(user.id);
            finish();
        } else {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters");
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            etPassword.setError("Password must contain at least one uppercase letter");
            return false;
        }
        if (!password.matches(".*\\d.*")) {
            etPassword.setError("Password must contain at least one digit");
            return false;
        }
        if (!password.matches(".*[@#$%^&+=!].*")) {
            etPassword.setError("Password must contain at least one special character (@#$%^&+=!)");
            return false;
        }
        return true;
    }

    private void navigateAfterAuth(long userId) {
        if (redirectToProfile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        }
    }
}
