package com.example.calculatrice;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "participations")
public class ParticipationEntity {

    @PrimaryKey(autoGenerate = false)
    public String id;

    public String eventId;
    public String fullName;
    public String email;
    public String phone;
    public String note;
    public long createdAt;
    public String status; // pending, accepted, refused
    public String qrCodeData;
    public long decisionAt;

    public ParticipationEntity() {}

    public ParticipationEntity(String eventId, String fullName, String email,
                               String phone, String note, long createdAt) {
        this.eventId = eventId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.note = note;
        this.createdAt = createdAt;
        this.status = "pending";
        this.qrCodeData = null;
        this.decisionAt = 0L;
    }
}

