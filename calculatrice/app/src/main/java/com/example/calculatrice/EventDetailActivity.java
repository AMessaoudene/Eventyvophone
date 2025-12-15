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
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    firestoreHelper.getParticipation(content, new FirestoreHelper.OnComplete<ParticipationEntity>() {
                        @Override
                        public void onSuccess(ParticipationEntity p) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            if (p != null && p.eventId != null && p.eventId.equals(event.id)) {
                                String msg = "Valid Ticket!\nPerson: " + p.fullName + "\nStatus: " + p.status;
                                int icon = "accepted".equalsIgnoreCase(p.status) ? android.R.drawable.checkbox_on_background : android.R.drawable.ic_delete;
                                new AlertDialog.Builder(EventDetailActivity.this)
                                        .setTitle("Verification Result")
                                        .setIcon(icon)
                                        .setMessage(msg)
                                        .setPositiveButton("OK", null)
                                        .show();
                            } else {
                                Toast.makeText(EventDetailActivity.this, "Invalid Ticket: Not for this event", Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override
                        public void onFailure(Exception e) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(EventDetailActivity.this, "Scan Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

    private android.widget.ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        firestoreHelper = new FirestoreHelper();
        userId = SessionManager.getUserId(this);

        event = (EventEntity) getIntent().getSerializableExtra("event");
        if (event == null) {
             String eventId = getIntent().getStringExtra("eventId");
             if (eventId == null) {
                 finish();
                 return;
             }
             Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
             finish();
             return;
        }

        progressBar = findViewById(R.id.progressBar);
        loadParticipationCount();
        bindEventDetails();
        setupBottomNav();
    }

    // ... loadParticipationCount and bindEventDetails ...

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete event")
                .setMessage("Do you really want to delete this event?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    firestoreHelper.deleteEvent(event.id, new FirestoreHelper.OnComplete<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(EventDetailActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(EventDetailActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
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
        Button btnShare = findViewById(R.id.btnShare);
        Button btnManage = findViewById(R.id.btnManageParticipation);
        Button btnAnalytics = findViewById(R.id.btnAnalytics);
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

        btnShare.setOnClickListener(v -> {
            String text = "Check out this event: " + event.name + "\n" +
                    "When: " + event.startDate + "\n" +
                    "About: " + event.description; 
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);
        });

        boolean canManage = isOwner && event.hasParticipationForm;
        btnManage.setVisibility(canManage ? View.VISIBLE : View.GONE);
        btnManage.setOnClickListener(v -> openManageParticipation());

        // Analytics Button Logic
        btnAnalytics.setVisibility(canManage ? View.VISIBLE : View.GONE);
        btnAnalytics.setOnClickListener(v -> showAnalyticsDialog());

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

    private void showAnalyticsDialog() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        firestoreHelper.getParticipations(event.id, new FirestoreHelper.OnComplete<java.util.List<ParticipationEntity>>() {
            @Override
            public void onSuccess(java.util.List<ParticipationEntity> list) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                
                int total = list.size();
                int accepted = 0;
                int refused = 0;
                int pending = 0;
                
                for (ParticipationEntity p : list) {
                    if ("accepted".equalsIgnoreCase(p.status)) accepted++;
                    else if ("refused".equalsIgnoreCase(p.status)) refused++;
                    else pending++;
                }

                // Inflate Custom Dialog
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_analytics, null);
                PieChartView pieChart = dialogView.findViewById(R.id.pieChart);
                TextView tvAccepted = dialogView.findViewById(R.id.tvLegendAccepted);
                TextView tvRefused = dialogView.findViewById(R.id.tvLegendRefused);
                TextView tvPending = dialogView.findViewById(R.id.tvLegendPending);
                TextView tvTitle = dialogView.findViewById(R.id.tvAnalyticsTitle);

                tvTitle.setText("Analytics (" + total + " Total)");

                // Set Data
                float[] data = new float[] { accepted, refused, pending };
                int[] colors = new int[] { 
                    0xFF4CAF50, // Green
                    0xFFF44336, // Red
                    0xFFFF9800  // Orange
                };
                pieChart.setData(data, colors);

                // Update Legend Text
                tvAccepted.setText("● Accepted: " + accepted);
                tvRefused.setText("● Refused: " + refused);
                tvPending.setText("● Pending: " + pending);
                
                new AlertDialog.Builder(EventDetailActivity.this)
                        .setView(dialogView)
                        .setPositiveButton("Close", null)
                        .setNeutralButton("Manage", (dialog, which) -> openManageParticipation())
                        .show();
            }

            @Override
            public void onFailure(Exception e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(EventDetailActivity.this, "Failed to load analytics", Toast.LENGTH_SHORT).show();
            }
        });
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
