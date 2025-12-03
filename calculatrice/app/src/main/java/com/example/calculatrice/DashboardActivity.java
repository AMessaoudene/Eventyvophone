package com.example.calculatrice;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etStartDate;
    private EditText etEndDate;
    private EditText etLocation;
    private EditText etMeetLink;
    private EditText etDescription;
    private CheckBox cbOnline;
    private CheckBox cbFree;
    private CheckBox cbParticipationForm;
    private Button btnPickPhoto;
    private Button btnCreateOrUpdateEvent;
    private Button btnShowEvents;
    private Button btnDeleteEvent;
    private ImageView imgPreview;
    private Uri selectedImageUri;
    private long userIdLong; // Keeping for compatibility if needed, but mainly using String UID
    private String userId;
    private FirestoreHelper firestoreHelper;
    private boolean isEditMode = false;
    private EventEntity eventToEdit;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            try {
                                getContentResolver().takePersistableUriPermission(selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                            imgPreview.setImageURI(selectedImageUri);
                        }
                    });

    private android.widget.ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        firestoreHelper = new FirestoreHelper();
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = SessionManager.getUserId(this);
        }
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bindViews();
        progressBar = findViewById(R.id.progressBar);
        setupListeners();
        checkForEditMode();
        setupBottomNav();
    }

    // ... bindViews and setupListeners ...

    private void saveEvent() {
        String name = etName.getText().toString().trim();
        String start = etStartDate.getText().toString().trim();
        String end = etEndDate.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        boolean online = cbOnline.isChecked();
        String location = online ? null : etLocation.getText().toString().trim();
        String meet = online ? etMeetLink.getText().toString().trim() : null;
        boolean free = cbFree.isChecked();
        boolean participation = cbParticipationForm.isChecked();

        if (name.isEmpty() || start.isEmpty() || end.isEmpty() || desc.isEmpty() ||
                (online && (meet == null || meet.isEmpty())) ||
                (!online && (location == null || location.isEmpty()))) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (online && meet != null && !android.util.Patterns.WEB_URL.matcher(meet).matches()) {
            etMeetLink.setError("Invalid URL");
            etMeetLink.requestFocus();
            return;
        }

        if (start.compareTo(end) > 0) {
            etEndDate.setError("End date cannot be before start date");
            etEndDate.requestFocus();
            return;
        }

        String imageUriToSave = selectedImageUri != null ? selectedImageUri.toString()
                : (isEditMode && eventToEdit != null ? eventToEdit.imageUri : null);
        if (imageUriToSave == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        if (isEditMode && eventToEdit != null) {
            eventToEdit.name = name;
            eventToEdit.startDate = start;
            eventToEdit.endDate = end;
            eventToEdit.description = desc;
            eventToEdit.isOnline = online;
            eventToEdit.location = location;
            eventToEdit.meetLink = meet;
            eventToEdit.isFree = free;
            eventToEdit.hasParticipationForm = participation;
            eventToEdit.imageUri = imageUriToSave;
            
            firestoreHelper.updateEvent(eventToEdit, new FirestoreHelper.OnComplete<Void>() {
                @Override
                public void onSuccess(Void result) {
                    showLoading(false);
                    Toast.makeText(DashboardActivity.this, "Event updated", Toast.LENGTH_SHORT).show();
                    NotificationHelper.showNotification(DashboardActivity.this, "Event Updated", "Event '" + name + "' has been updated.");
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    showLoading(false);
                    Toast.makeText(DashboardActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            EventEntity e = new EventEntity(name, start, end, location, meet, online, free,
                    desc, imageUriToSave, userId, participation);
            
            firestoreHelper.addEvent(e, new FirestoreHelper.OnComplete<Void>() {
                @Override
                public void onSuccess(Void result) {
                    showLoading(false);
                    Toast.makeText(DashboardActivity.this, "Event created!", Toast.LENGTH_SHORT).show();
                    NotificationHelper.showNotification(DashboardActivity.this, "Event Created", "Event '" + name + "' has been created.");
                    clearForm();
                }

                @Override
                public void onFailure(Exception e) {
                    showLoading(false);
                    Toast.makeText(DashboardActivity.this, "Creation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        btnCreateOrUpdateEvent.setEnabled(!isLoading);
        btnDeleteEvent.setEnabled(!isLoading);
    }

    // ... clearForm ...

    private void confirmDelete() {
        if (!isEditMode || eventToEdit == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete event")
                .setMessage("This action cannot be undone. Continue?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    showLoading(true);
                    firestoreHelper.deleteEvent(eventToEdit.id, new FirestoreHelper.OnComplete<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            showLoading(false);
                            Toast.makeText(DashboardActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            showLoading(false);
                            Toast.makeText(DashboardActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }
}
