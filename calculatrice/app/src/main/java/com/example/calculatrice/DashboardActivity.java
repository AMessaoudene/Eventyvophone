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
    private long userId;
    private AppDatabase db;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = AppDatabase.getInstance(this);
        userId = getIntent().getLongExtra("userId", -1);
        if (userId == -1L) {
            userId = SessionManager.getUserId(this);
        }
        if (userId == -1L) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bindViews();
        setupListeners();
        checkForEditMode();
        setupBottomNav();
    }

    private void bindViews() {
        etName = findViewById(R.id.etEventName);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etLocation = findViewById(R.id.etEventLocation);
        etMeetLink = findViewById(R.id.etMeetLink);
        etDescription = findViewById(R.id.etDescription);
        cbOnline = findViewById(R.id.cbOnline);
        cbFree = findViewById(R.id.cbFreeEntry);
        cbParticipationForm = findViewById(R.id.cbParticipationForm);
        btnPickPhoto = findViewById(R.id.btnPickPhoto);
        btnCreateOrUpdateEvent = findViewById(R.id.btnCreateEvent);
        btnShowEvents = findViewById(R.id.btnViewEvents);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
        imgPreview = findViewById(R.id.imgPreview);
    }

    private void setupListeners() {
        btnPickPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        cbOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etLocation.setVisibility(View.GONE);
                etMeetLink.setVisibility(View.VISIBLE);
            } else {
                etLocation.setVisibility(View.VISIBLE);
                etMeetLink.setVisibility(View.GONE);
            }
        });

        btnCreateOrUpdateEvent.setOnClickListener(v -> saveEvent());
        btnShowEvents.setOnClickListener(v -> startActivity(
                new Intent(this, EventListActivity.class)
                        .putExtra("userId", userId)
                        .putExtra("onlyMine", true)));
        btnDeleteEvent.setOnClickListener(v -> confirmDelete());
    }

    private void checkForEditMode() {
        long eventIdToEdit = getIntent().getLongExtra("eventId", -1);
        if (eventIdToEdit != -1) {
            isEditMode = true;
            eventToEdit = db.eventDao().getEventById(eventIdToEdit);
            if (eventToEdit == null || eventToEdit.organizerId != userId) {
                Toast.makeText(this, "Unable to edit this event", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            populateForm(eventToEdit);
        } else {
            isEditMode = false;
        }
        btnDeleteEvent.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        btnCreateOrUpdateEvent.setText(isEditMode ? "Update Event" : "Create Event");
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_events) {
                startActivity(new Intent(this, EventListActivity.class)
                        .putExtra("userId", userId)
                        .putExtra("onlyMine", true));
                return true;
            } else if (id == R.id.nav_profile) {
                long loggedId = SessionManager.getUserId(this);
                if (loggedId == -1L) {
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

    private void populateForm(EventEntity event) {
        etName.setText(event.name);
        etStartDate.setText(event.startDate);
        etEndDate.setText(event.endDate);
        cbOnline.setChecked(event.isOnline);
        if (event.isOnline) {
            etMeetLink.setVisibility(View.VISIBLE);
            etLocation.setVisibility(View.GONE);
            etMeetLink.setText(event.meetLink);
        } else {
            etMeetLink.setVisibility(View.GONE);
            etLocation.setVisibility(View.VISIBLE);
            etLocation.setText(event.location);
        }
        cbFree.setChecked(event.isFree);
        cbParticipationForm.setChecked(event.hasParticipationForm);
        etDescription.setText(event.description);
        if (event.imageUri != null) {
            try {
                selectedImageUri = Uri.parse(event.imageUri);
                imgPreview.setImageURI(selectedImageUri);
            } catch (Exception ignored) {
                imgPreview.setImageResource(android.R.color.transparent);
            }
        }
    }

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
            db.eventDao().update(eventToEdit);
            Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            EventEntity e = new EventEntity(name, start, end, location, meet, online, free,
                    desc, imageUriToSave, userId, participation);
            db.eventDao().insert(e);
            Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show();
            clearForm();
        }
    }

    private void clearForm() {
        etName.setText("");
        etStartDate.setText("");
        etEndDate.setText("");
        etLocation.setText("");
        etMeetLink.setText("");
        etDescription.setText("");
        cbOnline.setChecked(false);
        cbFree.setChecked(false);
        cbParticipationForm.setChecked(false);
        imgPreview.setImageResource(android.R.color.transparent);
        selectedImageUri = null;
    }

    private void confirmDelete() {
        if (!isEditMode || eventToEdit == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete event")
                .setMessage("This action cannot be undone. Continue?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.eventDao().delete(eventToEdit.id);
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }
}
