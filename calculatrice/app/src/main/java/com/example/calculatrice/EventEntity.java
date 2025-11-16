package com.example.calculatrice;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String date;
    public String location;
    public boolean isOnline;
    public boolean isFree;
    public String imageUri;
    public long organizerId;

    public EventEntity(String name, String date, String location,
                       boolean isOnline, boolean isFree, String imageUri, long organizerId) {
        this.name = name;
        this.date = date;
        this.location = location;
        this.isOnline = isOnline;
        this.isFree = isFree;
        this.imageUri = imageUri;
        this.organizerId = organizerId;
    }
}
