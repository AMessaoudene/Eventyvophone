package com.example.calculatrice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {


    private FirestoreHelper firestoreHelper;
    private User currentUser;
    private android.widget.ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String userId = SessionManager.getUserId(this);
        if (userId == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_REDIRECT_TO_PROFILE, true);
            startActivity(intent);
            finish();
            return;
        }

        firestoreHelper = new FirestoreHelper();

        TextView tvUsername = findViewById(R.id.tvProfileUsername);
        TextView tvUserId = findViewById(R.id.tvProfileId);
        EditText etNewUsername = findViewById(R.id.etNewUsername);
        EditText etNewEmail = findViewById(R.id.etNewEmail);
        EditText etNewPassword = findViewById(R.id.etNewPassword);
        EditText etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        Button btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        Button btnLogout = findViewById(R.id.btnLogout);
        progressBar = findViewById(R.id.progressBar);

        // Fetch user from Firestore
        firestoreHelper.getUser(userId, new FirestoreHelper.OnComplete<User>() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                tvUsername.setText(user.username);
                tvUserId.setText("User ID: " + userId);
                etNewEmail.setText(user.email);
            }

            @Override
            public void onFailure(Exception e) {
                tvUsername.setText("Error loading user");
                Toast.makeText(ProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnUpdateProfile.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(this, "User not loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            String newUsername = etNewUsername.getText().toString().trim();
            String newEmail = etNewEmail.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmNewPassword.getText().toString().trim();

            if (TextUtils.isEmpty(newUsername) && TextUtils.isEmpty(newPassword) && TextUtils.isEmpty(newEmail)) {
                Toast.makeText(this, "Nothing to update", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!TextUtils.isEmpty(newUsername)) {
                if (newUsername.length() < 4) {
                    Toast.makeText(this, "Username must be at least 4 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentUser.username = newUsername;
            }
            
            if (!TextUtils.isEmpty(newEmail)) {
                 currentUser.email = newEmail;
            }

            showLoading(true);

            // Save to Firestore
            firestoreHelper.addUser(userId, currentUser.username, currentUser.email, new FirestoreHelper.OnComplete<Void>() {
                @Override
                public void onSuccess(Void result) {
                    SessionManager.saveUser(ProfileActivity.this, userId, currentUser.username);
                    tvUsername.setText(currentUser.username);
                    etNewUsername.setText("");
                    etNewEmail.setText(currentUser.email);
                    
                    Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                    NotificationHelper.showNotification(ProfileActivity.this, "Profile Updated", "Your profile details have been updated.");
                    
                    // Password update (Firebase Auth) - nested to ensure sequence or parallel
                    if (!TextUtils.isEmpty(newPassword)) {
                        updatePassword(newPassword, confirmPassword);
                    } else {
                        showLoading(false);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    showLoading(false);
                    Toast.makeText(ProfileActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
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
                String loggedId = SessionManager.getUserId(this);
                if (loggedId == null) {
                    startActivity(new Intent(this, MainActivity.class));
                } else {
                    startActivity(new Intent(this, DashboardActivity.class));
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

    private void updatePassword(String newPassword, String confirmPassword) {
        if (newPassword.length() < 6) {
            showLoading(false);
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showLoading(false);
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().getCurrentUser().updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show();
                        ((EditText)findViewById(R.id.etNewPassword)).setText("");
                        ((EditText)findViewById(R.id.etConfirmNewPassword)).setText("");
                    } else {
                        Toast.makeText(this, "Password update failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        findViewById(R.id.btnUpdateProfile).setEnabled(!isLoading);
        findViewById(R.id.btnLogout).setEnabled(!isLoading);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
