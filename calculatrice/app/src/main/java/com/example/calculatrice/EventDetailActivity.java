package com.example.calculatrice;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class EventDetailActivity extends AppCompatActivity {

    private long eventId;
    private AppDatabase db;
    private EventEntity event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventId = getIntent().getLongExtra("eventId", -1);
        if (eventId == -1) finish();

        db = AppDatabase.getInstance(this);
        event = db.eventDao().getEventById(eventId);
        if (event == null) finish();

        ImageView imgEvent = findViewById(R.id.imgEvent);
        TextView tvName = findViewById(R.id.tvName);
        TextView tvStart = findViewById(R.id.tvStart);
        TextView tvEnd = findViewById(R.id.tvEnd);
        TextView tvLocation = findViewById(R.id.tvLocation);
        TextView tvMeet = findViewById(R.id.tvMeetLink);
        TextView tvOnline = findViewById(R.id.tvOnline);
        TextView tvFree = findViewById(R.id.tvFree);
        TextView tvDescription = findViewById(R.id.tvDescription);

        Button btnEdit = findViewById(R.id.btnEdit);
        Button btnDelete = findViewById(R.id.btnDelete);
        Button btnParticipate = findViewById(R.id.btnParticipate);

        tvName.setText(event.name);
        tvStart.setText("Start: " + event.startDate);
        tvEnd.setText("End: " + event.endDate);
        tvDescription.setText(event.description);
        tvOnline.setText(event.isOnline ? "Online" : "Offline");
        tvFree.setText(event.isFree ? "Free Entry" : "Paid");

        if (event.isOnline) {
            tvLocation.setVisibility(android.view.View.GONE);
            tvMeet.setVisibility(android.view.View.VISIBLE);
            tvMeet.setText("Meet link: " + (event.meetLink == null ? "-" : event.meetLink));
        } else {
            tvMeet.setVisibility(android.view.View.GONE);
            tvLocation.setVisibility(android.view.View.VISIBLE);
            tvLocation.setText("Location: " + (event.location == null ? "-" : event.location));
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

        // show/hide participate
        if (!event.hasParticipationForm) {
            btnParticipate.setVisibility(android.view.View.GONE);
        } else {
            btnParticipate.setOnClickListener(v -> {
                Intent i = new Intent(this, ParticipationActivity.class);
                i.putExtra("eventId", event.id);
                startActivity(i);
            });
        }

        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, EditEventActivity.class);
            i.putExtra("eventId", event.id);
            startActivity(i);
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete")
                    .setMessage("Delete this event?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        db.eventDao().delete(event.id);
                        Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_events);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(0,0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0,0);
                return true;
            }
            return true;
        });
    }
}
