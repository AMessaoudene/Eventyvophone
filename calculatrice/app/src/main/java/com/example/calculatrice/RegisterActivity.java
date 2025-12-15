package com.example.calculatrice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private FirebaseAuth mAuth;
    private FirestoreHelper firestoreHelper;
    private android.widget.ProgressBar progressBar;
    private boolean redirectToProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        firestoreHelper = new FirestoreHelper();
        redirectToProfile = getIntent().getBooleanExtra(LoginActivity.EXTRA_REDIRECT_TO_PROFILE, false);

        etUsername = findViewById(R.id.etRegUsername);
        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        progressBar = findViewById(R.id.progressBar);

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

        showLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        firestoreHelper.addUser(uid, username, email, password, new FirestoreHelper.OnComplete<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                showLoading(false);
                                SessionManager.saveUser(RegisterActivity.this, uid, username);
                                Toast.makeText(RegisterActivity.this, "Welcome " + username + "!", Toast.LENGTH_SHORT).show();
                                NotificationHelper.showNotification(RegisterActivity.this, "Welcome!", "Account created successfully for " + username);
                                navigateAfterAuth();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                showLoading(false);
                                Toast.makeText(RegisterActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        findViewById(R.id.btnCreateAccount).setEnabled(!isLoading);
        findViewById(R.id.btnBackToLogin).setEnabled(!isLoading);
    }

    private void navigateAfterAuth() {
        if (redirectToProfile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else {
            Intent intent = new Intent(this, DashboardActivity.class);
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

