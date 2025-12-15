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
    private FirestoreHelper firestoreHelper;
    private EventEntity event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participation);

        firestoreHelper = new FirestoreHelper();
        
        event = (EventEntity) getIntent().getSerializableExtra("event");
        if (event == null) {
            // Fallback if not passed (shouldn't happen with updated flow)
             String eventId = getIntent().getStringExtra("eventId");
             if (eventId == null) {
                 finish();
                 return;
             }
             // For now, we assume event is passed. If not, we can't check hasParticipationForm easily without fetching.
             // We'll proceed but might miss the check if we don't fetch.
             // Given the flow, we'll just show a toast if event is missing.
             Toast.makeText(this, "Error loading event info", Toast.LENGTH_SHORT).show();
             finish();
             return;
        }

        if (!event.hasParticipationForm) {
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
        
        firestoreHelper.addParticipation(entity, new FirestoreHelper.OnComplete<Void>() {
            @Override
            public void onSuccess(Void result) {
                String message = "Thanks " + fn + ", your participation";
                message += " for " + event.name + " is recorded.";
                Toast.makeText(ParticipationActivity.this, message, Toast.LENGTH_LONG).show();
                
                // 1. Notify the Participant (Local + Firestore)
                // We don't have the participant's User ID easily unless they are logged in.
                // But we can check SessionManager.
                String currentUserId = SessionManager.getUserId(ParticipationActivity.this);
                if (currentUserId != null) {
                    NotificationEntity userNotif = new NotificationEntity(
                        currentUserId,
                        "Participation Confirmed",
                        "You have successfully joined " + event.name
                    );
                    firestoreHelper.addNotification(userNotif, new FirestoreHelper.OnComplete<Void>() {
                        @Override
                        public void onSuccess(Void v) {} // Ignore
                        @Override
                        public void onFailure(Exception e) {}
                    });
                }
                // Local Notification always
                NotificationHelper.showNotification(ParticipationActivity.this, "Participation Confirmed", "You have successfully joined " + event.name);

                // 2. Notify the Organizer (Firestore only, they will see it in their bell)
                if (event.organizerId != null) {
                    NotificationEntity orgNotif = new NotificationEntity(
                        event.organizerId,
                        "New Participant",
                        fn + " has joined your event: " + event.name
                    );
                    firestoreHelper.addNotification(orgNotif, new FirestoreHelper.OnComplete<Void>() {
                        @Override
                        public void onSuccess(Void v) {}
                        @Override
                        public void onFailure(Exception e) {}
                    });
                }

                android.content.Intent intent = new android.content.Intent(ParticipationActivity.this, EventListActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ParticipationActivity.this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
