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
    private EventAdapter adapter;
    private FirestoreHelper firestoreHelper;
    private String userId;
    private boolean showOnlyMine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        firestoreHelper = new FirestoreHelper();
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = SessionManager.getUserId(this);
        }
        showOnlyMine = getIntent().getBooleanExtra("onlyMine", false) && userId != null;

        rvEvents = findViewById(R.id.rvEvents);
        tvEmptyState = findViewById(R.id.tvEmptyEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(null);
        adapter.setOnItemClickListener(event -> {
            // If the user is the organizer, go to Dashboard (Edit), else go to Details (View/Participate)
            // But wait, the original code went to EventDetailActivity.
            // Let's stick to EventDetailActivity for now, or check logic.
            // Actually, usually Details shows info and "Edit" button if owner.
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventId", event.id);
            intent.putExtra("event", event); // Pass object to avoid re-fetch
            startActivity(intent);
        });
        rvEvents.setAdapter(adapter);

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        FirestoreHelper.OnComplete<List<EventEntity>> callback = new FirestoreHelper.OnComplete<List<EventEntity>>() {
            @Override
            public void onSuccess(List<EventEntity> events) {
                adapter.updateData(events);
                boolean isEmpty = events == null || events.isEmpty();
                tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Exception e) {
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
