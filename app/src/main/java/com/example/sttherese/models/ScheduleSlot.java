package com.example.sttherese.models;

public class ScheduleSlot {

    private String date; // e.g., "Nov 17, 2025" (for display)
    private String day; // e.g., "Monday"
    private String startTime; // e.g., "9:00 AM"
    private String endTime; // e.g., "5:00 PM"
    private String status; // e.g., "Available"
    private String dayShort; // MON, TUE, etc. (for display)

    private String slotId; // (Retained, though unused for this daily view)

    // Empty constructor needed for Firestore deserialization
    public ScheduleSlot() {}

    // Constructor for the new daily schedule view
    public ScheduleSlot(String date, String day, String startTime, String endTime, String status) {
        this.date = date;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getDayShort() { return dayShort; }
    public void setDayShort(String ds) { this.dayShort = ds; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }
}