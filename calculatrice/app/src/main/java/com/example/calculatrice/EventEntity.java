package com.example.calculatrice;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String startDate;
    public String endDate;
    public String location;
    public String meetLink;
    public boolean isOnline;
    public boolean isFree;
    public String description;
    public String imageUri;
    public long organizerId;
    public boolean hasParticipationForm;

    public EventEntity(String name, String startDate, String endDate,
                       String location, String meetLink, boolean isOnline,
                       boolean isFree, String description, String imageUri,
                       long organizerId, boolean hasParticipationForm) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.meetLink = meetLink;
        this.isOnline = isOnline;
        this.isFree = isFree;
        this.description = description;
        this.imageUri = imageUri;
        this.organizerId = organizerId;
        this.hasParticipationForm = hasParticipationForm;
    }
}
