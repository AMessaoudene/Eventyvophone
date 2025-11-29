package com.example.calculatrice;

import android.os.Bundle;
import android.text.TextUtils;
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
    private AppDatabase db;
    private EventEntity event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participation);

        db = AppDatabase.getInstance(this);
        long eventId = getIntent().getLongExtra("eventId", -1L);
        if (eventId == -1L) {
            finish();
            return;
        }

        event = db.eventDao().getEventById(eventId);
        if (event == null || !event.hasParticipationForm) {
            Toast.makeText(this, "Participation is not available for this event", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        tvEventTitle.setText(event.name);
    }

    private void bindViews() {
        fullName = findViewById(R.id.etFullName);
        email = findViewById(R.id.etEmail);
        phone = findViewById(R.id.etPhone);
        note = findViewById(R.id.etNote);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        String fn = fullName.getText().toString().trim();
        String em = email.getText().toString().trim();
        String ph = phone.getText().toString().trim();
        String notes = note.getText().toString().trim();

        if (TextUtils.isEmpty(fn) || TextUtils.isEmpty(em)) {
            Toast.makeText(this, "Please enter name and email", Toast.LENGTH_SHORT).show();
            return;
        }

        ParticipationEntity entity = new ParticipationEntity(
                event.id,
                fn,
                em,
                ph,
                notes,
                System.currentTimeMillis()
        );
        db.participationDao().insert(entity);

        String message = "Thanks " + fn + ", your participation";
        message += " for " + event.name + " is recorded.";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}
