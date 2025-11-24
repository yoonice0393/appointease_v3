package com.example.sttherese.models;

public class Appointment {
    private String id;

    // Display fields (computed/fetched separately)
    private String doctorName;
    private String specialty;
    private String doctorAvatar;
    private String patientName;
    private String patientAvatar;
    private String service;

    // ---  FOR PATIENT DETAILS ---
    private String patientBirthday;
    private String patientAge;
    private String patientAddress;
    private String patientContact;


    // Firestore fields (from appointments collection)
    private String doctorId;        // Maps to "doctorId" in Firestore
    private String userId;          // Maps to "userId" in Firestore
    private String appointmentType; // Maps to "appointmentType" in Firestore
    private String date;
    private String time;
    private String status;

    // Empty constructor required for Firestore
    public Appointment() {}

    public Appointment(String id, String doctorName, String specialty, String doctorAvatar,
                       String patientName, String patientAvatar, String service,
                       String date, String time, String status) {
        this.id = id;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.doctorAvatar = doctorAvatar;
        this.patientName = patientName;
        this.patientAvatar = patientAvatar;
        this.service = service;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getDoctorAvatar() { return doctorAvatar; }
    public void setDoctorAvatar(String doctorAvatar) { this.doctorAvatar = doctorAvatar; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientAvatar() { return patientAvatar; }
    public void setPatientAvatar(String patientAvatar) { this.patientAvatar = patientAvatar; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // New getters/setters for Firestore fields
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPatientBirthday() { return patientBirthday; }
    public void setPatientBirthday(String patientBirthday) { this.patientBirthday = patientBirthday; }

    public String getPatientAge() { return patientAge; }
    public void setPatientAge(String patientAge) { this.patientAge = patientAge; }

    public String getPatientAddress() { return patientAddress; }
    public void setPatientAddress(String patientAddress) { this.patientAddress = patientAddress; }

    public String getPatientContact() { return patientContact; }
    public void setPatientContact(String patientContact) { this.patientContact = patientContact; }


    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) {
        this.appointmentType = appointmentType;
        // You might want to also set service field for backward compatibility
        this.service = appointmentType;
    }
}