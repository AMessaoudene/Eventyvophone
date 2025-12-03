package com.example.calculatrice;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {

    private FirestoreHelper firestoreHelper;
    private EventEntity event;
    private int participationCount;
    private String userId;

    private final androidx.activity.result.ActivityResultLauncher<Intent> scanLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String content = result.getData().getStringExtra("SCAN_RESULT");
                    Toast.makeText(this, "Scanned: " + content, Toast.LENGTH_LONG).show();
                    // TODO: Verify participation code
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        firestoreHelper = new FirestoreHelper();
        userId = SessionManager.getUserId(this);

        event = (EventEntity) getIntent().getSerializableExtra("event");
        if (event == null) {
            // Fallback: try to get ID and fetch (not implemented fully yet, relying on object pass)
             String eventId = getIntent().getStringExtra("eventId");
             if (eventId == null) {
                 finish();
                 return;
             }
             // Ideally we should fetch here if event is null, but for now we assume it's passed.
             // If we really need to fetch, we can add getEvent(id) to helper.
             Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
             finish();
             return;
        }

        loadParticipationCount();
        bindEventDetails();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload participation count or event details if needed
        if (event != null) {
             loadParticipationCount();
        }
    }

    private void loadParticipationCount() {
        firestoreHelper.countParticipations(event.id, new FirestoreHelper.OnComplete<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                participationCount = count;
                bindEventDetails(); // Refresh UI
            }

            @Override
            public void onFailure(Exception e) {
                // Ignore or show error
            }
        });
    }

    private void bindEventDetails() {
        ImageView imgEvent = findViewById(R.id.imgEvent);
        TextView tvName = findViewById(R.id.tvName);
        TextView tvStart = findViewById(R.id.tvStart);
        TextView tvEnd = findViewById(R.id.tvEnd);
        TextView tvLocation = findViewById(R.id.tvLocation);
        TextView tvMeet = findViewById(R.id.tvMeetLink);
        TextView tvOnline = findViewById(R.id.tvOnline);
        TextView tvFree = findViewById(R.id.tvFree);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvParticipation = findViewById(R.id.tvParticipationStatus);
        Button btnParticipate = findViewById(R.id.btnParticipate);
        Button btnManage = findViewById(R.id.btnManageParticipation);
        Button btnEdit = findViewById(R.id.btnEditEvent);
        Button btnDelete = findViewById(R.id.btnDeleteEvent);

        tvName.setText(event.name);
        tvStart.setText("Start: " + event.startDate);
        tvEnd.setText("End: " + event.endDate);
        tvDescription.setText(event.description);
        tvOnline.setText(event.isOnline ? "Online" : "Offline");
        tvFree.setText(event.isFree ? "Free Entry" : "Paid");

        if (event.isOnline) {
            tvLocation.setVisibility(View.GONE);
            tvMeet.setVisibility(View.VISIBLE);
            tvMeet.setText("Meet link: " + (event.meetLink == null ? "-" : event.meetLink));
        } else {
            tvMeet.setVisibility(View.GONE);
            tvLocation.setVisibility(View.VISIBLE);
            tvLocation.setText("Location: " + (event.location == null ? "-" : event.location));
        }

        if (event.hasParticipationForm) {
            String label = String.format(Locale.getDefault(),
                    "Participation form open • %d response%s recorded",
                    participationCount,
                    participationCount == 1 ? "" : "s");
            tvParticipation.setText(label);
        } else {
            tvParticipation.setText("Participation form is disabled.");
        }

        if (event.imageUri != null && !event.imageUri.isEmpty()) {
            try {
                imgEvent.setImageURI(Uri.parse(event.imageUri));
            } catch (Exception ex) {
                imgEvent.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            imgEvent.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        boolean isOwner = userId != null && userId.equals(event.organizerId);

        btnParticipate.setVisibility(event.hasParticipationForm ? View.VISIBLE : View.GONE);
        btnParticipate.setOnClickListener(v -> openParticipationForm());

        boolean canManage = isOwner && event.hasParticipationForm;
        btnManage.setVisibility(canManage ? View.VISIBLE : View.GONE);
        btnManage.setOnClickListener(v -> openManageParticipation());

        btnEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("eventId", event.id); // Still pass ID just in case
            intent.putExtra("event", event); // Pass object
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> confirmDelete());

        Button btnScanQr = findViewById(R.id.btnScanQr);
        if (isOwner) {
            btnScanQr.setVisibility(View.VISIBLE);
            btnScanQr.setOnClickListener(v -> {
                com.google.zxing.integration.android.IntentIntegrator integrator = new com.google.zxing.integration.android.IntentIntegrator(this);
                integrator.setCaptureActivity(ScanActivity.class);
                integrator.setOrientationLocked(false);
                integrator.setPrompt("Scan QR Code");
                Intent intent = integrator.createScanIntent();
                scanLauncher.launch(intent);
            });
        } else {
            btnScanQr.setVisibility(View.GONE);
        }
    }

    private void openParticipationForm() {
        Intent intent = new Intent(this, ParticipationActivity.class);
        intent.putExtra("eventId", event.id);
        intent.putExtra("eventName", event.name);
        intent.putExtra("event", event); // Pass object
        startActivity(intent);
    }

    private void openManageParticipation() {
        Intent intent = new Intent(this, ParticipationManageActivity.class);
        intent.putExtra("eventId", event.id);
        startActivity(intent);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete event")
                .setMessage("Do you really want to delete this event?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    firestoreHelper.deleteEvent(event.id, new FirestoreHelper.OnComplete<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(EventDetailActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(EventDetailActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_events);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if (SessionManager.getUserId(this) == null) {
                    startActivity(new Intent(this, MainActivity.class));
                } else {
                    startActivity(new Intent(this, DashboardActivity.class));
                }
                return true;
            } else if (id == R.id.nav_profile) {
                String userId = SessionManager.getUserId(this);
                if (userId == null) {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.putExtra(LoginActivity.EXTRA_REDIRECT_TO_PROFILE, true);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(this, ProfileActivity.class));
                }
                return true;
            }
            return true;
        });
    }
}
