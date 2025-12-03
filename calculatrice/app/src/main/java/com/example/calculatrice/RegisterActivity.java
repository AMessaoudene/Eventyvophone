package com.example.calculatrice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private AppDatabase db;
    private boolean redirectToProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = AppDatabase.getInstance(this);
        redirectToProfile = getIntent().getBooleanExtra(LoginActivity.EXTRA_REDIRECT_TO_PROFILE, false);

        etUsername = findViewById(R.id.etRegUsername);
        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);

        Button btnCreate = findViewById(R.id.btnCreateAccount);
        Button btnBackToLogin = findViewById(R.id.btnBackToLogin);

        btnCreate.setOnClickListener(v -> register());
        btnBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_REDIRECT_TO_PROFILE, redirectToProfile);
            startActivity(intent);
            finish();
        });
    }

    private void register() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email address");
            return;
        }

        if (username.length() < 4) {
            Toast.makeText(this, "Username must be at least 4 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        User existing = db.userDao().findByUsername(username);
        if (existing != null) {
            Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show();
            return;
        }
        
        User existingEmail = db.userDao().findByEmail(email);
        if (existingEmail != null) {
            Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show();
            return;
        }

        long userId = db.userDao().insert(new User(username, password, email));
        User created = db.userDao().getById(userId);
        SessionManager.saveUser(this, created);
        Toast.makeText(this, "Welcome " + username + "!", Toast.LENGTH_SHORT).show();
        NotificationHelper.showNotification(this, "Welcome!", "Account created successfully for " + username);


        if (redirectToProfile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        }
        finishAffinity();
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
}

