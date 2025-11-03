package com.example.sttherese.models;

public class Doctor {
    private String id;        // Firestore document ID
    private String name;
    private String specialty;
    private String scheduleDays;
    private String phone;
    private String notes;
    private String avatarUrl; // <-- Add this

    public Doctor() {
        // Needed for Firestore
    }

    public Doctor(String id, String name, String specialty, String scheduleDays, String phone, String notes, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.scheduleDays = scheduleDays;
        this.phone = phone;
        this.notes = notes;
        this.avatarUrl = avatarUrl;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getScheduleDays() { return scheduleDays; }
    public void setScheduleDays(String scheduleDays) { this.scheduleDays = scheduleDays; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
