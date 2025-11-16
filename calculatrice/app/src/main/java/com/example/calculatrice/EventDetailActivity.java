package com.example.calculatrice;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class EventDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        long eventId = getIntent().getLongExtra("eventId", -1);
        if (eventId == -1) finish();

        AppDatabase db = AppDatabase.getInstance(this);
        EventEntity event = db.eventDao().getEventById(eventId);
        if (event == null) finish();

        ImageView imgEvent = findViewById(R.id.imgEvent);
        TextView tvName = findViewById(R.id.tvName);
        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvLocation = findViewById(R.id.tvLocation);
        TextView tvOnline = findViewById(R.id.tvOnline);
        TextView tvFree = findViewById(R.id.tvFree);

        tvName.setText(event.name);
        tvDate.setText(event.date);
        tvLocation.setText(event.location);
        tvOnline.setText(event.isOnline ? "Online" : "Offline");
        tvFree.setText(event.isFree ? "Free Entry" : "Paid");

        if (event.imageUri != null && !event.imageUri.isEmpty())
            imgEvent.setImageURI(Uri.parse(event.imageUri));
        else
            imgEvent.setImageResource(android.R.drawable.ic_menu_report_image);
    }
}
