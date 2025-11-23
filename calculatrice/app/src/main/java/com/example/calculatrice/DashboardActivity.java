package com.example.calculatrice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private AppDatabase db;
    private long userId;
    private FloatingActionButton fabCreate;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = AppDatabase.getInstance(this);

        // Prefer userId from Intent, fallback to saved pref
        userId = getIntent().getLongExtra("userId", -1);
        if (userId == -1) {
            SharedPreferences sp = getSharedPreferences("auth", MODE_PRIVATE);
            userId = sp.getLong("userId", -1);
        } else {
            getSharedPreferences("auth", MODE_PRIVATE).edit().putLong("userId", userId).apply();
        }

        rvEvents = findViewById(R.id.rvEventsDashboard);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(new ArrayList<>());
        rvEvents.setAdapter(adapter);

        progressBar = findViewById(R.id.progressBar);

        fabCreate = findViewById(R.id.fabCreateEvent);
        fabCreate.setOnClickListener(v -> {
            Intent i = new Intent(this, CreateEventActivity.class);
            i.putExtra("userId", userId);
            startActivity(i);
        });

        adapter.setOnItemClickListener(event -> {
            Intent i = new Intent(this, EventDetailActivity.class);
            i.putExtra("eventId", event.id);
            startActivity(i);
        });

        // Long click -> quick actions (edit / delete)
        adapter.setOnItemLongClickListener(event -> {
            showItemActions(event);
        });

        // bottom nav
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_events) {
                startActivity(new Intent(this, EventListActivity.class).putExtra("userId", userId));
                return true;
            } else if (id == R.id.nav_profile) {
                long loggedId = getSharedPreferences("auth", MODE_PRIVATE).getLong("userId", -1);
                if (loggedId == -1L)
                    startActivity(new Intent(this, LoginActivity.class));
                else
                    startActivity(new Intent(this, ProfileActivity.class));
                return true;
            } else {
                return false;
            }
        });
    }

    private void showItemActions(EventEntity event) {
        String[] items = {"Edit", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle(event.name)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        // Edit
                        Intent i = new Intent(this, EditEventActivity.class);
                        i.putExtra("eventId", event.id);
                        startActivity(i);
                    } else {
                        // Delete confirmation
                        new AlertDialog.Builder(this)
                                .setTitle("Delete event")
                                .setMessage("Are you sure you want to delete \"" + event.name + "\"?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    db.eventDao().delete(event.id);
                                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                                    refreshList();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                }).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        progressBar.setVisibility(View.VISIBLE);
        List<EventEntity> events = db.eventDao().getEventsForUser(userId);
        adapter.setData(events);
        progressBar.setVisibility(View.GONE);
    }
}
