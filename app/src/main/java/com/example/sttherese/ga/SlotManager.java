package com.example.sttherese.ga;

import java.util.*;

/**
 * Manages time slot fragmentation and availability tracking
 */
public class SlotManager {
    private Map<String, List<TimeRange>> availableSlots; // key: "doctorId_day"

    public SlotManager(List<DoctorAvailability> doctors) {
        availableSlots = new HashMap<>();
        initializeSlots(doctors);
    }

    // Initialize slots from doctor availability
    private void initializeSlots(List<DoctorAvailability> doctors) {
        for (DoctorAvailability doctor : doctors) {
            for (String day : doctor.getWorkingDays()) {
                String key = doctor.getDoctorId() + "_" + day;
                List<TimeRange> slots = new ArrayList<>();

                // Deep copy the available slots
                for (TimeRange slot : doctor.getAvailableSlots()) {
                    slots.add(new TimeRange(slot.getStartMinutes(), slot.getEndMinutes()));
                }

                availableSlots.put(key, slots);
            }
        }
    }

    /**
     * Check if a time slot is available
     */
    public boolean isSlotAvailable(int doctorId, String day, int startTime, int durationMinutes) {
        String key = doctorId + "_" + day;
        List<TimeRange> slots = availableSlots.get(key);

        if (slots == null) return false;

        int endTime = startTime + durationMinutes;

        for (TimeRange slot : slots) {
            if (startTime >= slot.getStartMinutes() && endTime <= slot.getEndMinutes()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Book a time slot (fragments the slot if needed)
     */
    public boolean bookSlot(int doctorId, String day, int startTime, int durationMinutes) {
        String key = doctorId + "_" + day;
        List<TimeRange> slots = availableSlots.get(key);

        if (slots == null) return false;

        int endTime = startTime + durationMinutes;

        for (int i = 0; i < slots.size(); i++) {
            TimeRange slot = slots.get(i);

            // Check if appointment fits in this slot
            if (startTime >= slot.getStartMinutes() && endTime <= slot.getEndMinutes()) {
                slots.remove(i);

                // Add remaining fragments
                // Fragment before appointment
                if (startTime > slot.getStartMinutes()) {
                    slots.add(i, new TimeRange(slot.getStartMinutes(), startTime));
                    i++;
                }

                // Fragment after appointment
                if (endTime < slot.getEndMinutes()) {
                    slots.add(i, new TimeRange(endTime, slot.getEndMinutes()));
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Get all available slots for a doctor on a specific day
     */
    public List<TimeRange> getAvailableSlots(int doctorId, String day) {
        String key = doctorId + "_" + day;
        List<TimeRange> slots = availableSlots.get(key);

        if (slots == null) {
            return new ArrayList<>();
        }

        // Return a copy to prevent external modification
        List<TimeRange> copy = new ArrayList<>();
        for (TimeRange slot : slots) {
            copy.add(new TimeRange(slot.getStartMinutes(), slot.getEndMinutes()));
        }

        return copy;
    }

    /**
     * Find the best available slot based on preferences
     */
    public TimeRange findBestSlot(int doctorId, String day, int durationMinutes,
                                  Integer preferredStart, Integer preferredEnd) {
        List<TimeRange> slots = getAvailableSlots(doctorId, day);

        TimeRange bestSlot = null;
        int bestScore = Integer.MIN_VALUE;

        for (TimeRange slot : slots) {
            // Skip slots that can't fit the appointment
            if (slot.getEndMinutes() - slot.getStartMinutes() < durationMinutes) {
                continue;
            }

            int score = 0;

            // Prefer slots that match preferred time
            if (preferredStart != null && preferredEnd != null) {
                int slotMid = (slot.getStartMinutes() + slot.getEndMinutes()) / 2;
                int preferredMid = (preferredStart + preferredEnd) / 2;
                int distance = Math.abs(slotMid - preferredMid);
                score -= distance; // Lower distance = higher score
            }

            // Prefer earlier slots (morning preference)
            score -= slot.getStartMinutes() / 10;

            // Prefer larger slots (less fragmentation)
            score += (slot.getEndMinutes() - slot.getStartMinutes()) / 5;

            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    /**
     * Calculate total available time for a doctor on a specific day
     */
    public int getTotalAvailableMinutes(int doctorId, String day) {
        List<TimeRange> slots = getAvailableSlots(doctorId, day);
        int total = 0;

        for (TimeRange slot : slots) {
            total += slot.getEndMinutes() - slot.getStartMinutes();
        }

        return total;
    }

    /**
     * Get slot utilization statistics
     */
    public Map<String, Double> getUtilizationStats(List<DoctorAvailability> doctors) {
        Map<String, Double> stats = new HashMap<>();

        for (DoctorAvailability doctor : doctors) {
            int totalOriginal = 0;
            int totalRemaining = 0;

            for (String day : doctor.getWorkingDays()) {
                // Calculate original total
                for (TimeRange slot : doctor.getAvailableSlots()) {
                    totalOriginal += slot.getEndMinutes() - slot.getStartMinutes();
                }

                // Calculate remaining total
                totalRemaining += getTotalAvailableMinutes(doctor.getDoctorId(), day);
            }

            double utilization = totalOriginal > 0 ?
                    ((totalOriginal - totalRemaining) / (double) totalOriginal) * 100 : 0;

            stats.put(doctor.getDoctorName(), utilization);
        }

        return stats;
    }

    /**
     * Reset all slots to original state
     */
    public void reset(List<DoctorAvailability> doctors) {
        availableSlots.clear();
        initializeSlots(doctors);
    }

    /**
     * Print current slot availability
     */
    public void printSlotStatus() {
        System.out.println("\n=== Current Slot Availability ===");

        List<String> sortedKeys = new ArrayList<>(availableSlots.keySet());
        Collections.sort(sortedKeys);

        for (String key : sortedKeys) {
            List<TimeRange> slots = availableSlots.get(key);
            System.out.println(key + ":");

            if (slots.isEmpty()) {
                System.out.println("  No available slots");
            } else {
                for (TimeRange slot : slots) {
                    System.out.println("  " + slot);
                }
            }
        }
    }
}