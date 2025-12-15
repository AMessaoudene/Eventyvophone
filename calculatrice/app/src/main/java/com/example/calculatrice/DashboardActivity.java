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

        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
        imgPreview = findViewById(R.id.imgPreview);
    }

    private void setupListeners() {
        btnPickPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // Date Pickers
        View.OnClickListener dateClickListener = v -> {
            final EditText editText = (EditText) v;
            java.util.Calendar c = java.util.Calendar.getInstance();
            int year = c.get(java.util.Calendar.YEAR);
            int month = c.get(java.util.Calendar.MONTH);
            int day = c.get(java.util.Calendar.DAY_OF_MONTH);

            new android.app.DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
                // Format: YYYY-MM-DD
                String date = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", year1, month1 + 1, dayOfMonth);
                editText.setText(date);
            }, year, month, day).show();
        };

        etStartDate.setOnClickListener(dateClickListener);
        etStartDate.setFocusable(false); // Disable manual typing
        etStartDate.setClickable(true);

        etEndDate.setOnClickListener(dateClickListener);
        etEndDate.setFocusable(false); // Disable manual typing
        etEndDate.setClickable(true);

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



        btnDeleteEvent.setOnClickListener(v -> confirmDelete());
    }

    private void checkForEditMode() {
        eventToEdit = (EventEntity) getIntent().getSerializableExtra("event");
        if (eventToEdit != null) {
            isEditMode = true;
            if (!eventToEdit.organizerId.equals(userId)) {
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
        // Dashboard is not really a nav item anymore, it's a modal/form.
        // But we highlight nothing or maybe Home if we consider it part of home flow?
        // Actually, if we are in Dashboard, we are creating an event.
        // Let's uncheck all items or just leave it.
        bottomNav.getMenu().setGroupCheckable(0, false, true);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, EventListActivity.class);
                intent.putExtra(EventListActivity.EXTRA_MODE, EventListActivity.MODE_PUBLIC);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_events) {
                Intent intent = new Intent(this, EventListActivity.class);
                intent.putExtra(EventListActivity.EXTRA_MODE, EventListActivity.MODE_MINE);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
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
            eventToEdit.isPublic = true; // Ensure event is public
            
            firestoreHelper.updateEvent(eventToEdit, new FirestoreHelper.OnComplete<Void>() {
                @Override
                public void onSuccess(Void result) {
                    showLoading(false);
                    Toast.makeText(DashboardActivity.this, "Event updated", Toast.LENGTH_SHORT).show();
                    NotificationHelper.showNotification(DashboardActivity.this, "Event Updated", "Event '" + name + "' has been updated.");
                    android.content.Intent intent = new android.content.Intent(DashboardActivity.this, EventListActivity.class);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    // Preserve user ID if possible, though EventListActivity might handle it via SessionManager
                    intent.putExtra("userId", userId);
                    startActivity(intent);
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
            e.isPublic = true; // Ensure event is public
            
            firestoreHelper.addEvent(e, new FirestoreHelper.OnComplete<Void>() {
                @Override
                public void onSuccess(Void result) {
                    showLoading(false);
                    Toast.makeText(DashboardActivity.this, "Event created! ID: " + e.id, Toast.LENGTH_LONG).show();
                    NotificationHelper.showNotification(DashboardActivity.this, "Event Created", "Event '" + name + "' has been created.");
                    clearForm();
                    
                    // Auto-redirect to list to see it immediately
                    android.content.Intent intent = new android.content.Intent(DashboardActivity.this, EventListActivity.class);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
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
