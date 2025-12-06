package com.example.calculatrice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_REDIRECT_TO_PROFILE = "redirect_to_profile";

    private EditText etEmail;
    private EditText etPassword;
    private android.widget.ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirestoreHelper firestoreHelper;
    private boolean redirectToProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        firestoreHelper = new FirestoreHelper();
        redirectToProfile = getIntent().getBooleanExtra(EXTRA_REDIRECT_TO_PROFILE, false);

        etEmail = findViewById(R.id.etUsername); // Reusing ID but treating as email
        etEmail.setHint("Email");
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserAndNavigate(currentUser.getUid());
        }
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        // 1. Try Standard Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        fetchUserAndNavigate(mAuth.getCurrentUser().getUid());
                    } else {
                        // 2. Fallback: Try Shadow Auth (Firestore Check)
                        loginWithFirestore(email, password, task.getException());
                    }
                });
    }

    private void loginWithFirestore(String email, String password, Exception originalAuthError) {
        // First find the user by email
        firestoreHelper.getUserByEmail(email, new FirestoreHelper.OnComplete<String>() {
            @Override
            public void onSuccess(String uid) {
                // User exists, check password field
                firestoreHelper.getUser(uid, new FirestoreHelper.OnComplete<User>() {
                    @Override
                    public void onSuccess(User user) {
                        showLoading(false);
                        // Check if password matches shadow password
                        if (user != null && user.password != null && user.password.equals(password)) {
                            // SHADOW LOGIN SUCCESS!
                            Toast.makeText(LoginActivity.this, "Login Successful (Shadow Auth)", Toast.LENGTH_SHORT).show();
                            SessionManager.saveUser(LoginActivity.this, uid, user.username);
                            navigateAfterAuth();
                            finish();
                        } else {
                            // Password mismatch: Show original error
                            Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                // User not found
                Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserAndNavigate(String uid) {
        firestoreHelper.getUser(uid, new FirestoreHelper.OnComplete<User>() {
            @Override
            public void onSuccess(User user) {
                showLoading(false);
                if (user != null) {
                    SessionManager.saveUser(LoginActivity.this, uid, user.username);
                    navigateAfterAuth();
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                }
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Failed to fetch profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        findViewById(R.id.btnLogin).setEnabled(!isLoading);
        findViewById(R.id.btnRegister).setEnabled(!isLoading);
    }

    private void navigateAfterAuth() {
        if (redirectToProfile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else {
            Intent intent = new Intent(this, EventListActivity.class);
            intent.putExtra(EventListActivity.EXTRA_MODE, EventListActivity.MODE_PUBLIC);
            startActivity(intent);
        }
    }
}
