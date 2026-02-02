package com.example.sttherese.ga;

public class AppointmentRequest {
    private int patientId;
    private String serviceName;
    private String specialty;
    private int durationMinutes; // variable duration

    public AppointmentRequest(int patientId, String serviceName, String specialty, int durationMinutes) {
        this.patientId = patientId;
        this.serviceName = serviceName;
        this.specialty = specialty;
        this.durationMinutes = durationMinutes;
    }

    public int getPatientId() { return patientId; }
    public String getServiceName() { return serviceName; }
    public String getSpecialty() { return specialty; }
    public int getDurationMinutes() { return durationMinutes; }
}
