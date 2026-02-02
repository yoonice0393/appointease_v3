package com.example.sttherese.ga;

public class ScheduledAppointment {
    private int patientId;
    private DoctorAvailability doctor;
    private String day;
    private int startTimeMinutes;
    private int endTimeMinutes;

    public ScheduledAppointment(int patientId, DoctorAvailability doctor, String day, int startTimeMinutes, int endTimeMinutes) {
        this.patientId = patientId;
        this.doctor = doctor;
        this.day = day;
        this.startTimeMinutes = startTimeMinutes;
        this.endTimeMinutes = endTimeMinutes;
    }

    public int getPatientId() { return patientId; }
    public DoctorAvailability getDoctor() { return doctor; }
    public String getDay() { return day; }
    public int getStartTimeMinutes() { return startTimeMinutes; }
    public int getEndTimeMinutes() { return endTimeMinutes; }

    @Override
    public String toString() {
        int startHour = startTimeMinutes / 60;
        int startMin = startTimeMinutes % 60;
        int endHour = endTimeMinutes / 60;
        int endMin = endTimeMinutes % 60;
        return String.format("Patient %d -> %s (%s) on %s: %02d:%02d-%02d:%02d",
                patientId,
                doctor.getDoctorName(),
                doctor.getSpecialty(),
                day,
                startHour, startMin,
                endHour, endMin
        );
    }
}
