package com.example.calculatrice;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private NotificationAdapter adapter;
    private FirestoreHelper firestoreHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }

        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty = findViewById(R.id.tvEmptyNotifications);
        
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(null);
        rvNotifications.setAdapter(adapter);

        firestoreHelper = new FirestoreHelper();
        String userId = SessionManager.getUserId(this);

        if (userId != null) {
            loadNotifications(userId);
        } else {
            Toast.makeText(this, "Please login to view notifications", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadNotifications(String userId) {
        firestoreHelper.getUserNotifications(userId, new FirestoreHelper.OnComplete<List<NotificationEntity>>() {
            @Override
            public void onSuccess(List<NotificationEntity> result) {
                adapter.updateData(result);
                if (result.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvNotifications.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvNotifications.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(NotificationActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
