package com.example.calculatrice;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;

public class EventDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        long eventId = getIntent().getLongExtra("eventId", -1);
        if (eventId == -1) finish();

        AppDatabase db = AppDatabase.getInstance(this);
        EventEntity event = db.eventDao().getEventById(eventId);
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

        if (event.imageUri != null && !event.imageUri.isEmpty()) {
            try {
                imgEvent.setImageURI(Uri.parse(event.imageUri));
            } catch (Exception ex) {
                imgEvent.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            imgEvent.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // bottom nav
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_events);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new android.content.Intent(this, DashboardActivity.class));
                overridePendingTransition(0,0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new android.content.Intent(this, MainActivity.class));
                overridePendingTransition(0,0);
                return true;
            }
            return true;
        });
    }
}
