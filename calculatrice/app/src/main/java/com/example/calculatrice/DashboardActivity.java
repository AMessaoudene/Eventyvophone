package com.example.calculatrice;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    EditText etName, etDate, etLocation;
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
        etDate = findViewById(R.id.etEventDate);
        etLocation = findViewById(R.id.etEventLocation);
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

        btnCreateEvent.setOnClickListener(v -> createEvent());
        btnShowEvents.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventListActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    private void createEvent() {
        String name = etName.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (name.isEmpty() || date.isEmpty() || location.isEmpty() || selectedImageUri == null) {
            Toast.makeText(this, "Fill all fields and select image", Toast.LENGTH_SHORT).show();
            return;
        }

        EventEntity event = new EventEntity(name, date, location,
                cbOnline.isChecked(), cbFree.isChecked(),
                selectedImageUri.toString(), userId);

        db.eventDao().insert(event);
        Toast.makeText(this, "Event Created!", Toast.LENGTH_SHORT).show();
    }
}
