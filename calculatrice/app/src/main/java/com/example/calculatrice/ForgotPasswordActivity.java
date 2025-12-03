package com.example.calculatrice;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etCode;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private LinearLayout layoutVerifyCode;
    private LinearLayout layoutNewPassword;
    private Button btnSendCode;
    private Button btnVerifyCode;
    private Button btnReset;
    private AppDatabase db;
    private User foundUser;
    private String generatedCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        db = AppDatabase.getInstance(this);

        etEmail = findViewById(R.id.etResetEmail);
        etCode = findViewById(R.id.etResetCode);
        etPassword = findViewById(R.id.etResetPassword);
        etConfirmPassword = findViewById(R.id.etResetConfirmPassword);
        
        layoutVerifyCode = findViewById(R.id.layoutVerifyCode);
        layoutNewPassword = findViewById(R.id.layoutNewPassword);
        
        btnSendCode = findViewById(R.id.btnSendCode);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);
        btnReset = findViewById(R.id.btnResetPassword);
        Button btnBack = findViewById(R.id.btnBackToLogin);

        btnSendCode.setOnClickListener(v -> sendResetCode());
        btnVerifyCode.setOnClickListener(v -> verifyResetCode());
        btnReset.setOnClickListener(v -> resetPassword());
        btnBack.setOnClickListener(v -> finish());
    }

    private void sendResetCode() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return;
        }

        foundUser = db.userDao().findByEmail(email);
        if (foundUser != null) {
            generatedCode = String.valueOf((int)(Math.random() * 9000) + 1000);
            NotificationHelper.showNotification(this, "Password Reset Code", "Your reset code is: " + generatedCode);
            Toast.makeText(this, "Code sent to email (Check Notification)", Toast.LENGTH_LONG).show();
            
            etEmail.setEnabled(false);
            btnSendCode.setEnabled(false);
            layoutVerifyCode.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void verifyResetCode() {
        String code = etCode.getText().toString().trim();
        if (code.equals(generatedCode)) {
            layoutVerifyCode.setVisibility(View.GONE);
            layoutNewPassword.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Code verified", Toast.LENGTH_SHORT).show();
        } else {
            etCode.setError("Invalid code");
        }
    }

    private void resetPassword() {
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters");
            return;
        }
        
        if (!isValidPassword(password)) {
             return;
        }

        if (!password.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        foundUser.password = password;
        db.userDao().update(foundUser);
        
        NotificationHelper.showNotification(this, "Password Reset", "Your password has been successfully reset.");
        Toast.makeText(this, "Password reset successfully", Toast.LENGTH_SHORT).show();
        finish();
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
