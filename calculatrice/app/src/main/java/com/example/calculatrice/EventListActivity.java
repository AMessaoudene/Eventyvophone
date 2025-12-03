package com.example.calculatrice;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class EventListActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private TextView tvEmptyState;
    private TextView tvStatus;
    private EventAdapter adapter;
    private FirestoreHelper firestoreHelper;
    private String userId;
    private boolean showOnlyMine;

    private android.widget.ProgressBar progressBar;

    private com.google.android.material.switchmaterial.SwitchMaterial switchMyEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        firestoreHelper = new FirestoreHelper();
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = SessionManager.getUserId(this);
        }
        
        // Always start with showing ALL events (Public)
        showOnlyMine = false;

        rvEvents = findViewById(R.id.rvEvents);
        tvEmptyState = findViewById(R.id.tvEmptyEvents);
        progressBar = findViewById(R.id.progressBar);
        switchMyEvents = findViewById(R.id.switchMyEvents);
        tvStatus = findViewById(R.id.tvListStatus);
        com.google.android.material.floatingactionbutton.FloatingActionButton fab = findViewById(R.id.fabAddEvent);

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(null);
        adapter.setOnItemClickListener(event -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventId", event.id);
            intent.putExtra("event", event); // Pass object to avoid re-fetch
            startActivity(intent);
        });
        rvEvents.setAdapter(adapter);

        // Show FAB only if user is logged in
        if (userId != null) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
            
            // Show toggle only if logged in
            switchMyEvents.setVisibility(View.VISIBLE);
            switchMyEvents.setChecked(false); // Force unchecked by default
            switchMyEvents.setOnCheckedChangeListener((buttonView, isChecked) -> {
                showOnlyMine = isChecked;
                loadEvents();
            });
        } else {
            fab.setVisibility(View.GONE);
            switchMyEvents.setVisibility(View.GONE);
        }

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        
        if (showOnlyMine) {
            tvStatus.setText("VIEWING: MY EVENTS ONLY");
            tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FF9800")); // Orange
        } else {
            tvStatus.setText("VIEWING: ALL PUBLIC EVENTS");
            tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")); // Green
        }

        FirestoreHelper.OnComplete<List<EventEntity>> callback = new FirestoreHelper.OnComplete<List<EventEntity>>() {
            @Override
            public void onSuccess(List<EventEntity> events) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                adapter.updateData(events);
                boolean isEmpty = events == null || events.isEmpty();
                tvEmptyState.setText(showOnlyMine ? "You have no events." : "No public events found.");
                tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Exception e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                tvEmptyState.setText("Error loading events: " + e.getMessage());
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        };

        if (showOnlyMine && userId != null) {
            firestoreHelper.getEventsByOrganizer(userId, callback);
        } else {
            firestoreHelper.getEvents(callback);
        }
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
                    startActivity(new Intent(this, DashboardActivity.class).putExtra("userId", userId));
                }
                return true;
            } else if (id == R.id.nav_profile) {
                String loggedId = SessionManager.getUserId(this);
                if (loggedId == null) {
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
