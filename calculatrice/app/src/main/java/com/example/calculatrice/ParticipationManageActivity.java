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

    private AppDatabase db;
    private long eventId;
    private EventEntity event;
    private ParticipationManageAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participation_manage);

        db = AppDatabase.getInstance(this);
        eventId = getIntent().getLongExtra("eventId", -1L);
        long userId = SessionManager.getUserId(this);

        event = db.eventDao().getEventById(eventId);
        if (event == null || !event.hasParticipationForm || userId == -1L || event.organizerId != userId) {
            finish();
            return;
        }

        TextView tvTitle = findViewById(R.id.tvManageTitle);
        tvEmpty = findViewById(R.id.tvParticipationEmpty);
        RecyclerView rvList = findViewById(R.id.rvParticipationList);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ParticipationManageAdapter(this);
        rvList.setAdapter(adapter);

        tvTitle.setText("Participation requests • " + event.name);

        loadParticipations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadParticipations();
    }

    private void loadParticipations() {
        List<ParticipationEntity> entries = db.participationDao().getForEvent(eventId);
        adapter.submit(entries);
        tvEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAccept(ParticipationEntity entity) {
        if ("accepted".equals(entity.status)) {
            showQr(entity);
            return;
        }
        String qrData = generateQrPayload(entity);
        db.participationDao().updateStatus(entity.id, "accepted", qrData, System.currentTimeMillis());
        ParticipationEntity updated = db.participationDao().getById(entity.id);
        showQr(updated);
        loadParticipations();
    }

    @Override
    public void onRefuse(ParticipationEntity entity) {
        if ("refused".equals(entity.status)) {
            return;
        }
        db.participationDao().updateStatus(entity.id, "refused", null, System.currentTimeMillis());
        loadParticipations();
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
                .show();
    }

    private String generateQrPayload(ParticipationEntity entity) {
        return "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}

