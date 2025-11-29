package com.example.calculatrice;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = AppDatabase.getInstance(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        long existingUserId = SessionManager.getUserId(this);
        if (existingUserId != -1L) {
            navigateToDashboard(existingUserId);
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

        User user = db.userDao().login(username, password);
        if (user != null) {
            SessionManager.saveUser(this, user);
            navigateToDashboard(user.id);
            finish();
        } else {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToDashboard(long userId) {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }
}
