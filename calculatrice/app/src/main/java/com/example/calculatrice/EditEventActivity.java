package com.example.calculatrice;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

public class EditEventActivity extends AppCompatActivity {

    private EditText etName, etStartDate, etEndDate, etLocation, etMeetLink, etDescription;
    private CheckBox cbOnline, cbFree, cbParticipationForm;
    private ImageView imgPreview;
    private Button btnPickPhoto, btnSave;
    private Uri selectedImageUri;
    private AppDatabase db;
    private long eventId;
    private EventEntity current;

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
        setContentView(R.layout.activity_edit_event);

        db = AppDatabase.getInstance(this);
        eventId = getIntent().getLongExtra("eventId", -1);
        if (eventId == -1) finish();

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
        btnSave = findViewById(R.id.btnCreateEvent);

        btnPickPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        loadEvent();

        btnSave.setOnClickListener(v -> saveChanges());
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

    private void loadEvent() {
        current = db.eventDao().getEventById(eventId);
        if (current == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etName.setText(current.name);
        etStartDate.setText(current.startDate);
        etEndDate.setText(current.endDate);
        etDescription.setText(current.description);
        cbOnline.setChecked(current.isOnline);
        cbFree.setChecked(current.isFree);
        cbParticipationForm.setChecked(current.hasParticipationForm);
        if (current.isOnline) {
            etLocation.setVisibility(android.view.View.GONE);
            etMeetLink.setVisibility(android.view.View.VISIBLE);
            etMeetLink.setText(current.meetLink);
        } else {
            etLocation.setVisibility(android.view.View.VISIBLE);
            etMeetLink.setVisibility(android.view.View.GONE);
            etLocation.setText(current.location);
        }

        if (current.imageUri != null && !current.imageUri.isEmpty()) {
            try { imgPreview.setImageURI(Uri.parse(current.imageUri)); }
            catch (Exception ex) { imgPreview.setImageResource(android.R.drawable.ic_menu_report_image); }
        }
    }

    private void saveChanges() {
        String name = etName.getText().toString().trim();
        String start = etStartDate.getText().toString().trim();
        String end = etEndDate.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        boolean online = cbOnline.isChecked();
        String location = online ? null : etLocation.getText().toString().trim();
        String meet = online ? etMeetLink.getText().toString().trim() : null;
        boolean free = cbFree.isChecked();
        boolean participation = cbParticipationForm.isChecked();
        String imageUriStr = (selectedImageUri != null) ? selectedImageUri.toString() : current.imageUri;

        // Validate required fields
        if (name.isEmpty() || start.isEmpty() || end.isEmpty() || desc.isEmpty() ||
                (online && (meet == null || meet.isEmpty())) ||
                (!online && (location == null || location.isEmpty()))) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create updated EventEntity
        EventEntity updated = new EventEntity(
                name, start, end,
                location, meet,
                online, free,
                desc, imageUriStr,
                current.organizerId, participation
        );
        updated.id = eventId; // Set the ID so Room knows which row to update

        // Update in DB
        db.eventDao().update(updated);

        Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show();
        finish();
    }
}
