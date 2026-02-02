package com.example.sttherese.ga;

import java.util.*;

public class ComprehensiveMainSimulation {
    public static void main(String[] args) {
        // Run different test scenarios
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║  GENETIC ALGORITHM APPOINTMENT SCHEDULING SYSTEM      ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // Scenario 1: Basic scheduling
        runBasicScenario();

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Scenario 2: With patient preferences
        runPreferenceScenario();

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Scenario 3: High demand scenario
        runHighDemandScenario();

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Scenario 4: Edge cases
        runEdgeCaseScenario();
    }

    /**
     * Basic scenario with simple appointments
     */
    private static void runBasicScenario() {
        System.out.println("SCENARIO 1: Basic Scheduling");
        System.out.println("─".repeat(60));

        List<DoctorAvailability> doctors = createSampleDoctors();
        List<AppointmentRequest> requests = new ArrayList<>();

        // Simple requests
        requests.add(new AppointmentRequest(101, "Prenatal Checkup", "OB-GYNE", 15));
        requests.add(new AppointmentRequest(102, "Ultrasound", "OB-GYNE", 45));
        requests.add(new AppointmentRequest(103, "Baby Immunization", "Pediatrics", 20));
        requests.add(new AppointmentRequest(104, "Newborn Screening", "Pediatrics", 30));

        runScheduler(doctors, requests);
    }

    /**
     * Scenario with patient preferences
     */
    private static void runPreferenceScenario() {
        System.out.println("SCENARIO 2: Scheduling with Patient Preferences");
        System.out.println("─".repeat(60));

        List<DoctorAvailability> doctors = createSampleDoctors();
        List<AppointmentRequest> requests = new ArrayList<>();

        // Requests with preferences
        EnhancedAppointmentRequest req1 = new EnhancedAppointmentRequest(
                201, "Prenatal Checkup", "OB-GYNE", 15,
                Arrays.asList("Monday", "Wednesday"),
                540, 660, // 9:00 AM - 11:00 AM
                1 // High priority
        );
        requests.add(req1);

        EnhancedAppointmentRequest req2 = new EnhancedAppointmentRequest(
                202, "Post-natal Checkup", "OB-GYNE", 30,
                Arrays.asList("Friday"),
                600, 720, // 10:00 AM - 12:00 PM
                2
        );
        requests.add(req2);

        EnhancedAppointmentRequest req3 = new EnhancedAppointmentRequest(
                203, "Baby Checkup", "Pediatrics", 25,
                Arrays.asList("Monday", "Tuesday"),
                540, 720, // 9:00 AM - 12:00 PM
                3
        );
        requests.add(req3);

        System.out.println("Patient Preferences:");
        for (AppointmentRequest req : requests) {
            System.out.println("  " + req);
        }
        System.out.println();

        runScheduler(doctors, requests);
    }

    /**
     * High demand scenario with many appointments
     */
    private static void runHighDemandScenario() {
        System.out.println("SCENARIO 3: High Demand (Stress Test)");
        System.out.println("─".repeat(60));

        List<DoctorAvailability> doctors = createExpandedDoctors();
        List<AppointmentRequest> requests = new ArrayList<>();

        // Generate 20 appointments
        Random random = new Random(42); // Fixed seed for reproducibility
        String[] services = {"Prenatal Checkup", "Ultrasound", "Post-natal Visit",
                "Consultation", "Follow-up"};
        String[] specialties = {"OB-GYNE", "Pediatrics"};
        int[] durations = {15, 20, 30, 45, 60};

        for (int i = 0; i < 20; i++) {
            String specialty = specialties[random.nextInt(specialties.length)];
            String service = services[random.nextInt(services.length)];
            int duration = durations[random.nextInt(durations.length)];

            requests.add(new AppointmentRequest(300 + i, service, specialty, duration));
        }

        System.out.println("Generated " + requests.size() + " appointment requests");
        System.out.println();

        runScheduler(doctors, requests);
    }

    /**
     * Edge cases and error handling
     */
    private static void runEdgeCaseScenario() {
        System.out.println("SCENARIO 4: Edge Cases & Error Handling");
        System.out.println("─".repeat(60));

        List<DoctorAvailability> doctors = createSampleDoctors();
        List<AppointmentRequest> requests = new ArrayList<>();

        // Valid requests
        requests.add(new AppointmentRequest(401, "Pelvic Examination", "OB-GYNE", 30));

        // Request for non-existent specialty
        requests.add(new AppointmentRequest(402, "Dental Cleaning", "Dentistry", 45));

        // Very long appointment
        requests.add(new AppointmentRequest(403, "Complex Surgery", "OB-GYNE", 180));

        // Short appointment
        requests.add(new AppointmentRequest(404, "Quick Consultation", "Pediatrics", 10));

        runScheduler(doctors, requests);
    }

    /**
     * Create sample doctors
     */
    private static List<DoctorAvailability> createSampleDoctors() {
        List<DoctorAvailability> doctors = new ArrayList<>();

        // Dr. Liza - OB-GYNE
        doctors.add(new DoctorAvailability(
                1, "Dr. Liza Ramos", "OB-GYNE",
                Arrays.asList("Monday", "Tuesday","Wednesday", "Friday"),
                Arrays.asList(
                        new TimeRange(480, 720),   // 8:00 AM - 12:00 PM
                        new TimeRange(780, 1020)   // 1:00 PM - 5:00 PM
                )
        ));

        // Dr. Ana - Pediatrics
        doctors.add(new DoctorAvailability(
                2, "Dr. Ana Reyes", "Pediatrics",
                Arrays.asList("Monday", "Tuesday", "Thursday","Saturday"),
                Arrays.asList(
                        new TimeRange(540, 840)    // 9:00 AM - 2:00 PM
                )
        ));

        return doctors;
    }

    /**
     * Create expanded list of doctors for stress testing
     */
    private static List<DoctorAvailability> createExpandedDoctors() {
        List<DoctorAvailability> doctors = new ArrayList<>();

        // OB-GYNE doctors
        doctors.add(new DoctorAvailability(
                1, "Dr. Maria Santos", "OB-GYNE",
                Arrays.asList("Monday", "Wednesday",  "Thursday", "Friday"),
                Arrays.asList(
                        new TimeRange(480, 720),
                        new TimeRange(780, 1020)
                )
        ));

        doctors.add(new DoctorAvailability(
                2, "Dr. Liza Ramos", "OB-GYNE",
                Arrays.asList("Monday", "Tuesday", "Friday"),
                Arrays.asList(
                        new TimeRange(540, 780),
                        new TimeRange(840, 1080)
                )
        ));

        // Pediatrics doctors
        doctors.add(new DoctorAvailability(
                3, "Dr. Ana Reyes", "Pediatrics",
                Arrays.asList("Monday", "Tuesday", "Wednesday","Thursday"),
                Arrays.asList(
                        new TimeRange(540, 840)
                )
        ));

        doctors.add(new DoctorAvailability(
                4, "Dr. Patel", "Pediatrics",
                Arrays.asList("Wednesday", "Thursday", "Friday"),
                Arrays.asList(
                        new TimeRange(480, 720),
                        new TimeRange(780, 960)
                )
        ));

        return doctors;
    }

    /**
     * Run the scheduler and display results
     */
    private static void runScheduler(List<DoctorAvailability> doctors,
                                     List<AppointmentRequest> requests) {
        GeneticAlgorithmScheduler ga =
                new GeneticAlgorithmScheduler(doctors, requests);

        long startTime = System.currentTimeMillis();
        List<ScheduledAppointment> schedule = ga.runSimulation();
        long endTime = System.currentTimeMillis();

        System.out.println("\n=== FINAL SCHEDULE ===");
        if (schedule.isEmpty()) {
            System.out.println("No appointments were scheduled.");
        } else {
            // Sort by doctor and time
            schedule.sort(Comparator
                    .comparing((ScheduledAppointment a) -> a.getDoctor().getDoctorName())
                    .thenComparing(ScheduledAppointment::getDay)
                    .thenComparing(ScheduledAppointment::getStartTimeMinutes));

            String currentDoctor = "";
            String currentDay = "";

            for (ScheduledAppointment appt : schedule) {
                String doctor = appt.getDoctor().getDoctorName();
                String day = appt.getDay();

                if (!doctor.equals(currentDoctor)) {
                    System.out.println("\n" + doctor + " (" + appt.getDoctor().getSpecialty() + "):");
                    currentDoctor = doctor;
                    currentDay = "";
                }

                if (!day.equals(currentDay)) {
                    System.out.println("  " + day + ":");
                    currentDay = day;
                }

                System.out.printf("    %s - Patient %d (%d min)%n",
                        formatTime(appt.getStartTimeMinutes()),
                        appt.getPatientId(),
                        appt.getEndTimeMinutes() - appt.getStartTimeMinutes());
            }
        }

        // Performance metrics
        System.out.println("\n=== PERFORMANCE METRICS ===");
        System.out.println("Execution time: " + (endTime - startTime) + " ms");
        System.out.println("Scheduled appointments: " + schedule.size() + "/" + requests.size());

        // Calculate success rate
        double successRate = requests.isEmpty() ? 0 :
                (schedule.size() / (double) requests.size()) * 100;
        System.out.printf("Success rate: %.1f%%%n", successRate);

        // Slot utilization with SlotManager
        SlotManager slotManager = new SlotManager(doctors);
        for (ScheduledAppointment appt : schedule) {
            slotManager.bookSlot(
                    appt.getDoctor().getDoctorId(),
                    appt.getDay(),
                    appt.getStartTimeMinutes(),
                    appt.getEndTimeMinutes() - appt.getStartTimeMinutes()
            );
        }

        Map<String, Double> utilization = slotManager.getUtilizationStats(doctors);
        System.out.println("\n=== SLOT UTILIZATION ===");
        for (Map.Entry<String, Double> entry : utilization.entrySet()) {
            System.out.printf("%s: %.1f%%%n", entry.getKey(), entry.getValue());
        }
    }

    /**
     * Format time in minutes to HH:MM AM/PM
     */
    private static String formatTime(int minutes) {
        int hour = minutes / 60;
        int min = minutes % 60;
        String period = hour < 12 ? "AM" : "PM";
        int displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
        return String.format("%d:%02d %s", displayHour, min, period);
    }
}