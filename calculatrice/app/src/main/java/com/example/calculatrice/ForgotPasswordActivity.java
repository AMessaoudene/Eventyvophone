package com.example.calculatrice;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendCode;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etResetEmail);
        btnSendCode = findViewById(R.id.btnSendCode);
        Button btnBack = findViewById(R.id.btnBackToLogin);

        // Hide unused views from previous implementation if they exist in layout
        // Or better, we should update the layout. For now, we just use what we need.
        if (findViewById(R.id.layoutVerifyCode) != null) findViewById(R.id.layoutVerifyCode).setVisibility(View.GONE);
        if (findViewById(R.id.layoutNewPassword) != null) findViewById(R.id.layoutNewPassword).setVisibility(View.GONE);

        btnSendCode.setText("Send Reset Email");
        btnSendCode.setOnClickListener(v -> sendResetEmail());
        btnBack.setOnClickListener(v -> finish());
    }

    private void sendResetEmail() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Reset email sent. Check your inbox.", Toast.LENGTH_LONG).show();
                        NotificationHelper.showNotification(this, "Reset Email Sent", "Check your email to reset password.");
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
