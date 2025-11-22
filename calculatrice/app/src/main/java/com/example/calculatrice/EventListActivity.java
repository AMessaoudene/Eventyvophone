package com.example.calculatrice;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventListActivity extends AppCompatActivity {

    RecyclerView rvEvents;
    AppDatabase db;
    long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        // Get current user ID
        userId = getIntent().getLongExtra("userId", -1);
        db = AppDatabase.getInstance(this);

        rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        // Load events for this user
        List<EventEntity> events = db.eventDao().getEventsForUser(userId);

        // Setup adapter
        EventAdapter adapter = new EventAdapter(events);
        rvEvents.setAdapter(adapter);

        // Handle click on items
        adapter.setOnItemClickListener(event -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventId", event.id);
            startActivity(intent);
        });

        // bottom nav
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_events);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class).putExtra("userId", userId));
                overridePendingTransition(0,0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0,0);
                return true;
            }
            return true;
        });
    }
}
