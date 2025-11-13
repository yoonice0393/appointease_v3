package com.example.sttherese.models;

public class Doctor {
    private String id;
    private String name;
    private String specialty;
    private String image;
    private String contact_number;
    private String remarks;
    private String avatarUrl;
    private String availability_type;
    private String scheduleDays;  // Add this field
    private String user_id;        // Add this field
    private boolean is_active;

    // Empty constructor required for Firestore
    public Doctor() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getContact_number() { return contact_number; }
    public void setContact_number(String contact_number) { this.contact_number = contact_number; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getAvailability_type() { return availability_type; }
    public void setAvailability_type(String availability_type) { this.availability_type = availability_type; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getScheduleDays() { return scheduleDays; }
    public void setScheduleDays(String scheduleDays) { this.scheduleDays = scheduleDays; }

    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    public boolean isIs_active() { return is_active; }
    public void setIs_active(boolean is_active) { this.is_active = is_active; }
}