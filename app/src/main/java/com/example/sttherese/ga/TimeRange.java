package com.example.sttherese.ga;

public class TimeRange {
    private int startMinutes; // minutes from 00:00
    private int endMinutes;

    public TimeRange(int startMinutes, int endMinutes) {
        this.startMinutes = startMinutes;
        this.endMinutes = endMinutes;
    }

    public int getStartMinutes() { return startMinutes; }
    public int getEndMinutes() { return endMinutes; }

    @Override
    public String toString() {
        int startHour = startMinutes / 60;
        int startMin = startMinutes % 60;
        int endHour = endMinutes / 60;
        int endMin = endMinutes % 60;
        return String.format("%02d:%02d-%02d:%02d", startHour, startMin, endHour, endMin);
    }
}
