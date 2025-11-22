package com.example.calculatrice;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ParticipationActivity extends AppCompatActivity {

    EditText fullName, email, phone, note;
    Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participation);

        fullName = findViewById(R.id.etFullName);
        email = findViewById(R.id.etEmail);
        phone = findViewById(R.id.etPhone);
        note = findViewById(R.id.etNote);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> {
            String fn = fullName.getText().toString().trim();
            String em = email.getText().toString().trim();

            if (fn.isEmpty() || em.isEmpty()) {
                Toast.makeText(this, "Please enter name and email", Toast.LENGTH_SHORT).show();
                return;
            }

            // For now just show toast. You can save to DB or send to server later.
            Toast.makeText(this, "Thanks " + fn + ", your participation is recorded.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
