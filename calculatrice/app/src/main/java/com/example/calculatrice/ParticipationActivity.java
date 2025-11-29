package com.example.calculatrice;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ParticipationActivity extends AppCompatActivity {

    private EditText fullName;
    private EditText email;
    private EditText phone;
    private EditText note;
    private TextView tvEventTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participation);

        fullName = findViewById(R.id.etFullName);
        email = findViewById(R.id.etEmail);
        phone = findViewById(R.id.etPhone);
        note = findViewById(R.id.etNote);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        String eventName = getIntent().getStringExtra("eventName");
        if (eventName != null) {
            tvEventTitle.setText(eventName);
        }

        btnSubmit.setOnClickListener(v -> handleSubmit(eventName));
    }

    private void handleSubmit(String eventName) {
        String fn = fullName.getText().toString().trim();
        String em = email.getText().toString().trim();

        if (fn.isEmpty() || em.isEmpty()) {
            Toast.makeText(this, "Please enter name and email", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Thanks " + fn + ", your participation";
        if (eventName != null) {
            message += " for " + eventName;
        }
        message += " is recorded.";

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}
