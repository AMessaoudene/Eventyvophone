package com.example.calculatrice;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calculatrice.qr.SimpleQrGenerator;

import java.util.List;
import java.util.UUID;

public class ParticipationManageActivity extends AppCompatActivity implements ParticipationManageAdapter.Listener {

    private FirestoreHelper firestoreHelper;
    private String eventId;
    private EventEntity event;
    private ParticipationManageAdapter adapter;
    private android.widget.ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participation_manage);

        firestoreHelper = new FirestoreHelper();
        eventId = getIntent().getStringExtra("eventId");
        String userId = SessionManager.getUserId(this);

        if (eventId == null || userId == null) {
            finish();
            return;
        }

        tvEmpty = findViewById(R.id.tvParticipationEmpty);
        progressBar = findViewById(R.id.progressBar);
        RecyclerView rvList = findViewById(R.id.rvParticipationList);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ParticipationManageAdapter(this);
        rvList.setAdapter(adapter);

        loadParticipations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadParticipations();
    }

    private void loadParticipations() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        firestoreHelper.getParticipations(eventId, new FirestoreHelper.OnComplete<List<ParticipationEntity>>() {
            @Override
            public void onSuccess(List<ParticipationEntity> entries) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                adapter.submit(entries);
                tvEmpty.setText("No participation requests yet.");
                tvEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Exception e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                tvEmpty.setText("Error loading participations: " + e.getMessage());
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onAccept(ParticipationEntity entity) {
        if ("accepted".equals(entity.status)) {
            showQr(entity);
            return;
        }
        String qrData = generateQrPayload(entity);
        entity.status = "accepted";
        entity.qrCodeData = qrData;
        entity.decisionAt = System.currentTimeMillis();
        
        firestoreHelper.updateParticipation(entity, new FirestoreHelper.OnComplete<Void>() {
            @Override
            public void onSuccess(Void result) {
                showQr(entity);
                loadParticipations();
                NotificationHelper.showNotification(ParticipationManageActivity.this, "Participation Accepted", "Accepted participation for " + entity.fullName);
            }

            @Override
            public void onFailure(Exception e) {
                android.widget.Toast.makeText(ParticipationManageActivity.this, "Failed to update: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRefuse(ParticipationEntity entity) {
        if ("refused".equals(entity.status)) {
            return;
        }
        entity.status = "refused";
        entity.decisionAt = System.currentTimeMillis();
        
        firestoreHelper.updateParticipation(entity, new FirestoreHelper.OnComplete<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadParticipations();
            }

            @Override
            public void onFailure(Exception e) {
                android.widget.Toast.makeText(ParticipationManageActivity.this, "Failed to update: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewProfile(ParticipationEntity entity) {
        TextView detail = new TextView(this);
        detail.setPadding(32, 32, 32, 32);
        String text = "Name: " + entity.fullName +
                "\nEmail: " + entity.email +
                (entity.phone == null || entity.phone.isEmpty() ? "" : "\nPhone: " + entity.phone) +
                (entity.note == null || entity.note.isEmpty() ? "" : "\nNote: " + entity.note);
        detail.setText(text);
        new AlertDialog.Builder(this)
                .setTitle("Participant details")
                .setView(detail)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showQr(ParticipationEntity entity) {
        if (entity.qrCodeData == null || entity.qrCodeData.isEmpty()) {
            return;
        }
        Bitmap bitmap = SimpleQrGenerator.generate(entity.qrCodeData, 600);
        ImageView imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setImageBitmap(bitmap);
        new AlertDialog.Builder(this)
                .setTitle("QR Code for " + entity.fullName)
                .setView(imageView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Save", (dialog, which) -> saveToGallery(bitmap, "QR_" + entity.fullName))
                .show();
    }

    private void saveToGallery(Bitmap bitmap, String title) {
        String savedImageURL = android.provider.MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                title,
                "QR Code for " + title
        );
        if (savedImageURL != null) {
            android.widget.Toast.makeText(this, "QR Code saved to Gallery", android.widget.Toast.LENGTH_SHORT).show();
        } else {
            android.widget.Toast.makeText(this, "Failed to save QR Code", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private String generateQrPayload(ParticipationEntity entity) {
        return "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}

