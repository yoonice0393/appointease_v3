package com.example.sttherese;

public class Doctor {
    private int id;
    private String name;
    private String specialty;
    private String hours;
    private String type;
    private int imageResource;

    public Doctor(int id, String name, String specialty, String hours, String type, int imageResource) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.hours = hours;
        this.type = type;
        this.imageResource = imageResource;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getSpecialty() { return specialty; }
    public String getHours() { return hours; }
    public String getType() { return type; }
    public int getImageResource() { return imageResource; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public void setHours(String hours) { this.hours = hours; }
    public void setType(String type) { this.type = type; }
    public void setImageResource(int imageResource) { this.imageResource = imageResource; }
}
