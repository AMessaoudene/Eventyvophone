package com.example.calculatrice;

import java.io.Serializable;

public class NotificationEntity implements Serializable {
    public String id;
    public String userId; // The recipient
    public String title;
    public String message;
    public long timestamp;
    public boolean isRead;

    public NotificationEntity() {}

    public NotificationEntity(String userId, String title, String message) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }
}
