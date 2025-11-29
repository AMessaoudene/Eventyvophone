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
    private AppDatabase db;
    private long userId;
    private boolean showOnlyMine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        db = AppDatabase.getInstance(this);
        userId = getIntent().getLongExtra("userId", -1L);
        if (userId == -1L) {
            userId = SessionManager.getUserId(this);
        }
        showOnlyMine = getIntent().getBooleanExtra("onlyMine", false) && userId != -1L;

        rvEvents = findViewById(R.id.rvEvents);
        tvEmptyState = findViewById(R.id.tvEmptyEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(null);
        adapter.setOnItemClickListener(event -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventId", event.id);
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
        List<EventEntity> events;
        if (showOnlyMine && userId != -1L) {
            events = db.eventDao().getEventsForUser(userId);
        } else {
            events = db.eventDao().getAll();
        }

        adapter.updateData(events);
        boolean isEmpty = events == null || events.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
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
                    startActivity(new Intent(this, DashboardActivity.class).putExtra("userId", userId));
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
