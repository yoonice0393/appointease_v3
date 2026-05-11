package com.example.sttherese.models;

import com.google.firebase.database.PropertyName;
import com.google.firebase.database.Exclude;

public class Notification {
    private String id;
    private String userId;  // Add this field
    private String title;
    private String message;
    private boolean isRead;
    private long timestamp;
    private String type;    // Add this field
    private String status;  // Add this field

    public Notification() {} // Firestore/RTDB needs empty constructor

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Exclude
    public boolean isRead() { return isRead; }

    @PropertyName("isRead")
    public boolean getIsRead() { return isRead; }

    @PropertyName("isRead")
    public void setIsRead(boolean isRead) { this.isRead = isRead; }

    @Exclude
    public void setRead(boolean read) { this.isRead = read; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
