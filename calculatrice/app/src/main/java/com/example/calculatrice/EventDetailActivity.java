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

public class EventDetailActivity extends AppCompatActivity {

    private AppDatabase db;
    private EventEntity event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        long eventId = getIntent().getLongExtra("eventId", -1);
        if (eventId == -1L) {
            finish();
            return;
        }

        db = AppDatabase.getInstance(this);
        event = db.eventDao().getEventById(eventId);
        if (event == null) {
            finish();
            return;
        }

        bindEventDetails();
        setupBottomNav();
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

        tvParticipation.setText(event.hasParticipationForm ?
                "Participation form is available for visitors." :
                "Participation form is disabled.");

        if (event.imageUri != null && !event.imageUri.isEmpty()) {
            try {
                imgEvent.setImageURI(Uri.parse(event.imageUri));
            } catch (Exception ex) {
                imgEvent.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            imgEvent.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        long loggedUserId = SessionManager.getUserId(this);
        boolean isOwner = loggedUserId != -1L && loggedUserId == event.organizerId;

        btnParticipate.setVisibility(event.hasParticipationForm ? View.VISIBLE : View.GONE);
        btnParticipate.setOnClickListener(v -> openParticipationForm());

        btnEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("eventId", event.id);
            intent.putExtra("userId", loggedUserId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void openParticipationForm() {
        Intent intent = new Intent(this, ParticipationActivity.class);
        intent.putExtra("eventId", event.id);
        intent.putExtra("eventName", event.name);
        startActivity(intent);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete event")
                .setMessage("Do you really want to delete this event?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.eventDao().delete(event.id);
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_events);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if (SessionManager.getUserId(this) == -1L) {
                    startActivity(new Intent(this, MainActivity.class));
                } else {
                    startActivity(new Intent(this, DashboardActivity.class));
                }
                return true;
            } else if (id == R.id.nav_profile) {
                if (SessionManager.getUserId(this) == -1L) {
                    startActivity(new Intent(this, LoginActivity.class));
                } else {
                    startActivity(new Intent(this, ProfileActivity.class));
                }
                return true;
            }
            return true;
        });
    }
}
