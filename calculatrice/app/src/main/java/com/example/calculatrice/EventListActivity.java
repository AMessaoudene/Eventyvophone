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

import android.widget.Toast;

public class EventListActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String MODE_PUBLIC = "PUBLIC";
    public static final String MODE_MINE = "MINE";

    private String currentMode = MODE_PUBLIC;

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

        // Determine Mode
        String modeExtra = getIntent().getStringExtra(EXTRA_MODE);
        if (modeExtra != null) {
            currentMode = modeExtra;
        } else {
            // Default to Public if not specified
            currentMode = MODE_PUBLIC;
        }

        // Force showOnlyMine based on mode
        showOnlyMine = MODE_MINE.equals(currentMode);

        rvEvents = findViewById(R.id.rvEvents);
        tvEmptyState = findViewById(R.id.tvEmptyEvents);
        progressBar = findViewById(R.id.progressBar);
        switchMyEvents = findViewById(R.id.switchMyEvents);
        tvStatus = findViewById(R.id.tvListStatus);
        com.google.android.material.floatingactionbutton.FloatingActionButton fab = findViewById(R.id.fabAddEvent);
        
        // Notification Bell Logic
        android.widget.ImageView btnNotifications = findViewById(R.id.btnNotifications);
        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationActivity.class));
        });

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(null);
        adapter.setOnItemClickListener(event -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventId", event.id);
            intent.putExtra("event", event);
            startActivity(intent);
        });
        rvEvents.setAdapter(adapter);

        // FAB Logic
        if (userId != null) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        } else {
            fab.setVisibility(View.GONE);
        }

        // Toggle Logic - Hide it! We use tabs now.
        switchMyEvents.setVisibility(View.GONE); 

        // Search Logic
        androidx.appcompat.widget.SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                return true;
            }
        });

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
        updateUIForMode();
    }

    private void updateUIForMode() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (MODE_MINE.equals(currentMode)) {
            tvStatus.setText("MY EVENTS (Organizer)");
            tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FF9800")); // Orange
            bottomNav.setSelectedItemId(R.id.nav_events);
        } else {
            tvStatus.setText("ACCUEIL - PUBLIC EVENTS");
            tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")); // Green
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void loadEvents() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        
        FirestoreHelper.OnComplete<List<EventEntity>> callback = new FirestoreHelper.OnComplete<List<EventEntity>>() {
            @Override
            public void onSuccess(List<EventEntity> events) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(EventListActivity.this, "DEBUG: Fetched " + events.size() + " events.", Toast.LENGTH_SHORT).show();
                adapter.updateData(events);
                boolean isEmpty = events == null || events.isEmpty();
                
                String emptyMsg = showOnlyMine ? "You have no events." : "No public events found. (Check internet)";
                tvEmptyState.setText(emptyMsg);
                tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                
                if (isEmpty) {
                    Toast.makeText(EventListActivity.this, showOnlyMine ? "No events found for you." : "No public events retrieved.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                String errorMsg = "Error loading events: " + e.getMessage();
                tvEmptyState.setText(errorMsg);
                tvEmptyState.setVisibility(View.VISIBLE);
                Toast.makeText(EventListActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        };

        if (showOnlyMine && userId != null) {
            firestoreHelper.getEventsByOrganizer(userId, callback);
        } else {
            // Use Real-time listener for Public events
            // We need to manage the listener lifecycle, but for now we just attach it
            firestoreHelper.listenToEvents(new FirestoreHelper.OnComplete<List<EventEntity>>() {
                @Override
                public void onSuccess(List<EventEntity> events) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    String debugMsg = "Mode: " + (showOnlyMine ? "MINE" : "PUBLIC") + "\nFetched: " + events.size();
                    Toast.makeText(EventListActivity.this, debugMsg, Toast.LENGTH_LONG).show();
                    
                    adapter.updateData(events);
                    boolean isEmpty = events == null || events.isEmpty();
                    
                    String emptyMsg = showOnlyMine ? "You have no events." : "No public events found. (Check internet)";
                    tvEmptyState.setText(emptyMsg);
                    tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onFailure(Exception e) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    String errorMsg = "Error loading events: " + e.getMessage();
                    tvEmptyState.setText(errorMsg);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    Toast.makeText(EventListActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }



    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        // Selection is handled in updateUIForMode
        
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if (!MODE_PUBLIC.equals(currentMode)) {
                    Intent intent = new Intent(this, EventListActivity.class);
                    intent.putExtra(EXTRA_MODE, MODE_PUBLIC);
                    intent.putExtra("userId", userId);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); 
                    startActivity(intent);
                    finish(); // Close current activity to avoid stacking
                    overridePendingTransition(0, 0); // No animation
                }
                return true;
            } else if (id == R.id.nav_events) {
                if (userId == null) {
                     Toast.makeText(this, "Please login to view your events", Toast.LENGTH_SHORT).show();
                     startActivity(new Intent(this, LoginActivity.class));
                     return false;
                }
                if (!MODE_MINE.equals(currentMode)) {
                    Intent intent = new Intent(this, EventListActivity.class);
                    intent.putExtra(EXTRA_MODE, MODE_MINE);
                    intent.putExtra("userId", userId);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(0, 0);
                }
                return true;
            } else if (id == R.id.nav_profile) {
                if (userId == null) {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.putExtra(LoginActivity.EXTRA_REDIRECT_TO_PROFILE, true);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(this, ProfileActivity.class));
                }
                return true;
            }
            return false;
        });
    }
}
