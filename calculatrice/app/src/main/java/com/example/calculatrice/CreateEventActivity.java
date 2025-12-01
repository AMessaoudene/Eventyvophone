package com.example.calculatrice;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class CreateEventActivity extends AppCompatActivity {

    private EditText etName, etStartDate, etEndDate, etLocation, etMeetLink, etDescription;
    private CheckBox cbOnline, cbFree, cbParticipationForm;
    private ImageView imgPreview;
    private Uri selectedImageUri;
    private long userId;
    private AppDatabase db;
    private Button btnPickPhoto, btnCreate;

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
        setContentView(R.layout.activity_create_event);

        db = AppDatabase.getInstance(this);
        userId = getIntent().getLongExtra("userId", -1);

        etName = findViewById(R.id.etEventName);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etLocation = findViewById(R.id.etEventLocation);
        etMeetLink = findViewById(R.id.etMeetLink);
        etDescription = findViewById(R.id.etDescription);
        cbOnline = findViewById(R.id.cbOnline);
        cbFree = findViewById(R.id.cbFreeEntry);
        cbParticipationForm = findViewById(R.id.cbParticipationForm);
        imgPreview = findViewById(R.id.imgPreview);
        btnPickPhoto = findViewById(R.id.btnPickPhoto);
        btnCreate = findViewById(R.id.btnCreateEvent);

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        cbOnline.setOnCheckedChangeListener((b, isChecked) -> {
            if (isChecked) {
                etLocation.setVisibility(View.GONE);
                etMeetLink.setVisibility(View.VISIBLE);
            } else {
                etLocation.setVisibility(View.VISIBLE);
                etMeetLink.setVisibility(View.GONE);
            }
        });

        btnPickPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnCreate.setOnClickListener(v -> createEvent());
    }

    private void showDatePicker(final EditText target) {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog dpd = new DatePickerDialog(this, (DatePicker view, int year, int month, int day) -> {
            String mm = String.format("%02d", month + 1);
            String dd = String.format("%02d", day);
            target.setText(year + "-" + mm + "-" + dd);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dpd.show();
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

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        EventEntity e = new EventEntity(name, start, end, location, meet, online, free,
                desc, selectedImageUri.toString(), userId, participation);
        db.eventDao().insert(e);
        Toast.makeText(this, "Event Created!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
