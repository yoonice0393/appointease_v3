package com.example.sttherese.models;

public class Appointment {
    private int id;
    private String doctorName;
    private String specialty;
    private String date;
    private String time;
    private String doctorAvatar;
    private String status;

    public Appointment() {}

    public Appointment(int id, String doctorName, String specialty, String date, String time, String doctorAvatar, String status) {
        this.id = id;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.date = date;
        this.time = time;
        this.doctorAvatar = doctorAvatar;
        this.status = status;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDoctorAvatar() { return doctorAvatar; }
    public void setDoctorAvatar(String doctorAvatar) { this.doctorAvatar = doctorAvatar; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}