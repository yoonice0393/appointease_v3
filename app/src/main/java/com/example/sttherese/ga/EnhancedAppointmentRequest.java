package com.example.sttherese.ga;

import java.util.ArrayList;
import java.util.List;

public class EnhancedAppointmentRequest extends AppointmentRequest {
    private List<String> preferredDays; // e.g., ["Monday", "Wednesday"]
    private Integer preferredStartTime; // in minutes, e.g., 540 for 9:00 AM
    private Integer preferredEndTime;   // in minutes, e.g., 720 for 12:00 PM
    private int priority; // 1 = highest, 5 = lowest
    private String notes;

    public EnhancedAppointmentRequest(int patientId, String serviceName,
                                      String specialty, int durationMinutes) {
        super(patientId, serviceName, specialty, durationMinutes);
        this.preferredDays = new ArrayList<>();
        this.priority = 3; // default medium priority
        this.notes = "";
    }

    public EnhancedAppointmentRequest(int patientId, String serviceName,
                                      String specialty, int durationMinutes,
                                      List<String> preferredDays,
                                      Integer preferredStartTime,
                                      Integer preferredEndTime,
                                      int priority) {
        super(patientId, serviceName, specialty, durationMinutes);
        this.preferredDays = preferredDays != null ? new ArrayList<>(preferredDays) : new ArrayList<>();
        this.preferredStartTime = preferredStartTime;
        this.preferredEndTime = preferredEndTime;
        this.priority = Math.max(1, Math.min(5, priority)); // clamp between 1-5
        this.notes = "";
    }

    // Getters
    public List<String> getPreferredDays() {
        return new ArrayList<>(preferredDays);
    }

    public Integer getPreferredStartTime() {
        return preferredStartTime;
    }

    public Integer getPreferredEndTime() {
        return preferredEndTime;
    }

    public int getPriority() {
        return priority;
    }

    public String getNotes() {
        return notes;
    }

    // Setters
    public void setPreferredDays(List<String> preferredDays) {
        this.preferredDays = new ArrayList<>(preferredDays);
    }

    public void setPreferredStartTime(Integer preferredStartTime) {
        this.preferredStartTime = preferredStartTime;
    }

    public void setPreferredEndTime(Integer preferredEndTime) {
        this.preferredEndTime = preferredEndTime;
    }

    public void setPriority(int priority) {
        this.priority = Math.max(1, Math.min(5, priority));
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes : "";
    }

    // Helper method to check if time is within preferred range
    public boolean isTimePreferred(int timeMinutes) {
        if (preferredStartTime == null || preferredEndTime == null) {
            return true; // no preference = all times acceptable
        }
        return timeMinutes >= preferredStartTime && timeMinutes <= preferredEndTime;
    }

    // Helper method to check if day is preferred
    public boolean isDayPreferred(String day) {
        if (preferredDays.isEmpty()) {
            return true; // no preference = all days acceptable
        }
        return preferredDays.contains(day);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Patient %d - %s (%s) - %d min - Priority %d",
                getPatientId(), getServiceName(), getSpecialty(),
                getDurationMinutes(), priority));

        if (!preferredDays.isEmpty()) {
            sb.append(" - Preferred Days: ").append(preferredDays);
        }

        if (preferredStartTime != null && preferredEndTime != null) {
            sb.append(String.format(" - Preferred Time: %02d:%02d-%02d:%02d",
                    preferredStartTime / 60, preferredStartTime % 60,
                    preferredEndTime / 60, preferredEndTime % 60));
        }

        return sb.toString();
    }
}