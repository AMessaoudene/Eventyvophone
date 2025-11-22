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

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    EditText etName, etStartDate, etEndDate, etLocation, etMeetLink, etDescription;
    CheckBox cbOnline, cbFree;
    Button btnPickPhoto, btnCreateEvent, btnShowEvents;
    ImageView imgPreview;
    Uri selectedImageUri = null;
    long userId;
    AppDatabase db;

    ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
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
        btnPickPhoto = findViewById(R.id.btnPickPhoto);
        imgPreview = findViewById(R.id.imgPreview);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnShowEvents = findViewById(R.id.btnViewEvents);

        btnPickPhoto.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(pickIntent);
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

        btnShowEvents.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventListActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        // bottom navigation setup
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_events) {
                startActivity(new Intent(this, EventListActivity.class).putExtra("userId", userId));
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

    private void createEvent() {
        String name = etName.getText().toString().trim();
        String start = etStartDate.getText().toString().trim();
        String end = etEndDate.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        boolean online = cbOnline.isChecked();
        String location = online ? null : etLocation.getText().toString().trim();
        String meetLink = online ? etMeetLink.getText().toString().trim() : null;

        if (name.isEmpty() || start.isEmpty() || end.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!online && (location == null || location.isEmpty())) {
            Toast.makeText(this, "Enter location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (online && (meetLink == null || meetLink.isEmpty())) {
            Toast.makeText(this, "Enter meet link", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        EventEntity event = new EventEntity(
                name,
                start,
                end,
                location,
                meetLink,
                online,
                cbFree.isChecked(),
                description,
                selectedImageUri.toString(),
                userId
        );

        db.eventDao().insert(event);
        Toast.makeText(this, "Event Created!", Toast.LENGTH_SHORT).show();

        // optionally clear form
        etName.setText("");
        etStartDate.setText("");
        etEndDate.setText("");
        etDescription.setText("");
        etLocation.setText("");
        etMeetLink.setText("");
        imgPreview.setImageResource(android.R.color.transparent);
        selectedImageUri = null;
    }
}
