package com.example.sttherese.ga;

import java.util.List;

public class DoctorAvailability {
    private int doctorId;
    private String doctorName;
    private String specialty;
    private List<String> workingDays; // e.g., ["Monday", "Wednesday"]
    private List<TimeRange> availableSlots; // per day slots

    public DoctorAvailability(int doctorId, String doctorName, String specialty,
                              List<String> workingDays, List<TimeRange> availableSlots) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.workingDays = workingDays;
        this.availableSlots = availableSlots;
    }

    public int getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName; }
    public String getSpecialty() { return specialty; }
    public List<String> getWorkingDays() { return workingDays; }
    public List<TimeRange> getAvailableSlots() { return availableSlots; }
}
