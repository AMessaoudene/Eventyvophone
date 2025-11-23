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
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    private EditText etName, etStartDate, etEndDate, etLocation, etMeetLink, etDescription;
    private CheckBox cbOnline, cbFree, cbParticipationForm;
    private Button btnPickPhoto, btnCreateEvent, btnShowEvents;
    private ImageView imgPreview;
    private Uri selectedImageUri;
    private long userId;
    private AppDatabase db;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            imgPreview.setImageURI(selectedImageUri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        userId = getIntent().getLongExtra("userId", -1);
        db = AppDatabase.getInstance(this);

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
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnShowEvents = findViewById(R.id.btnViewEvents);
        imgPreview = findViewById(R.id.imgPreview);

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

        btnCreateEvent.setOnClickListener(v -> createEvent());

        btnShowEvents.setOnClickListener(v -> startActivity(new Intent(this, EventListActivity.class)
                .putExtra("userId", userId)));

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

    private void createEvent() {
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

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        EventEntity e = new EventEntity(name, start, end, location, meet, online, free,
                desc, selectedImageUri.toString(), userId, participation);
        db.eventDao().insert(e);
        Toast.makeText(this, "Event Created!", Toast.LENGTH_SHORT).show();
        clearForm();
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
}
