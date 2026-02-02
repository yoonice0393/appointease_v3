package com.example.sttherese.ga;

import android.util.Log;
import java.util.*;

/**
 * GA Simulation Runner with Multiple Scenarios
 */
public class GASimulationRunner {
    private static final String TAG = "GASimulation";

    public enum Scenario {
        BASIC,
        PATIENT_PREFERENCES,
        HIGH_DEMAND,
        EDGE_CASES
    }

    /**
     * Run simulation with a selected scenario
     */
    public static SimulationResult runSimulation(Scenario scenario) {
        Log.d(TAG, "Running GA Simulation for scenario: " + scenario);

        List<DoctorAvailability> doctors;
        List<AppointmentRequest> requests;

        switch (scenario) {
            case BASIC:
                doctors = createSampleDoctors();
                requests = createBasicRequests();
                break;
            case PATIENT_PREFERENCES:
                doctors = createSampleDoctors();
                requests = createPreferenceRequests();
                break;
            case HIGH_DEMAND:
                doctors = createExpandedDoctors();
                requests = createHighDemandRequests();
                break;
            case EDGE_CASES:
                doctors = createSampleDoctors();
                requests = createEdgeCaseRequests();
                break;
            default:
                doctors = createSampleDoctors();
                requests = createBasicRequests();
        }

        // Run GA
        GeneticAlgorithmScheduler ga = new GeneticAlgorithmScheduler(doctors, requests);
        long startTime = System.currentTimeMillis();
        List<ScheduledAppointment> schedule = ga.runSimulation();
        long endTime = System.currentTimeMillis();

        SimulationResult result = new SimulationResult();
        result.schedule = schedule;
        result.executionTimeMs = endTime - startTime;
        result.totalRequests = requests.size();
        result.scheduledCount = schedule.size();
        result.metrics = ga.getMetrics();
        result.doctors = doctors;

        Log.d(TAG, "Simulation complete: " + schedule.size() + "/" + requests.size() + " scheduled");
        return result;
    }

    /*** SAMPLE DATA METHODS ***/
    private static List<DoctorAvailability> createSampleDoctors() {
        List<DoctorAvailability> doctors = new ArrayList<>();
        doctors.add(new DoctorAvailability(1, "Dr. Liza Ramos", "OB-GYNE",
                Arrays.asList("Monday", "Tuesday","Wednesday","Friday"),
                Arrays.asList(new TimeRange(480,720), new TimeRange(780,1020))));
        doctors.add(new DoctorAvailability(2, "Dr. Ana Reyes", "Pediatrics",
                Arrays.asList("Monday","Tuesday","Thursday","Saturday"),
                Arrays.asList(new TimeRange(540,840))));
        return doctors;
    }

    private static List<DoctorAvailability> createExpandedDoctors() {
        List<DoctorAvailability> doctors = new ArrayList<>();
        // Add multiple doctors for stress testing
        doctors.add(new DoctorAvailability(1,"Dr. Maria Santos","OB-GYNE",
                Arrays.asList("Monday","Wednesday","Thursday","Friday"),
                Arrays.asList(new TimeRange(480,720),new TimeRange(780,1020))));
        doctors.add(new DoctorAvailability(2,"Dr. Liza Ramos","OB-GYNE",
                Arrays.asList("Monday","Tuesday","Friday"),
                Arrays.asList(new TimeRange(540,780),new TimeRange(840,1080))));
        doctors.add(new DoctorAvailability(3,"Dr. Ana Reyes","Pediatrics",
                Arrays.asList("Monday","Tuesday","Wednesday","Thursday"),
                Arrays.asList(new TimeRange(540,840))));
        doctors.add(new DoctorAvailability(4,"Dr. Patel","Pediatrics",
                Arrays.asList("Wednesday","Thursday","Friday"),
                Arrays.asList(new TimeRange(480,720),new TimeRange(780,960))));
        return doctors;
    }

    private static List<AppointmentRequest> createBasicRequests() {
        return Arrays.asList(
                new AppointmentRequest(101,"Prenatal Checkup","OB-GYNE",15),
                new AppointmentRequest(102,"Ultrasound","OB-GYNE",45),
                new AppointmentRequest(103,"Baby Immunization","Pediatrics",20)
        );
    }

    private static List<AppointmentRequest> createPreferenceRequests() {
        List<AppointmentRequest> requests = new ArrayList<>();
        requests.add(new EnhancedAppointmentRequest(201,"Prenatal Checkup","OB-GYNE",15,
                Arrays.asList("Monday","Wednesday"),540,660,1));
        requests.add(new EnhancedAppointmentRequest(202,"Post-natal Checkup","OB-GYNE",30,
                Arrays.asList("Friday"),600,720,2));
        requests.add(new EnhancedAppointmentRequest(203,"Baby Checkup","Pediatrics",25,
                Arrays.asList("Monday","Tuesday"),540,720,3));
        return requests;
    }

    private static List<AppointmentRequest> createHighDemandRequests() {
        List<AppointmentRequest> requests = new ArrayList<>();
        Random rand = new Random(42);
        String[] services = {"Prenatal Checkup","Ultrasound","Post-natal Visit","Consultation","Follow-up"};
        String[] specialties = {"OB-GYNE","Pediatrics"};
        int[] durations = {15,20,30,45,60};
        for (int i=0;i<20;i++){
            requests.add(new AppointmentRequest(300+i,
                    services[rand.nextInt(services.length)],
                    specialties[rand.nextInt(specialties.length)],
                    durations[rand.nextInt(durations.length)]));
        }
        return requests;
    }

    private static List<AppointmentRequest> createEdgeCaseRequests() {
        List<AppointmentRequest> requests = new ArrayList<>();
        requests.add(new AppointmentRequest(401,"Pelvic Examination","OB-GYNE",30));
        requests.add(new AppointmentRequest(402,"Dental Cleaning","Dentistry",45)); // non-existent specialty
        requests.add(new AppointmentRequest(403,"Complex Surgery","OB-GYNE",180)); // long duration
        requests.add(new AppointmentRequest(404,"Quick Consultation","Pediatrics",10));
        return requests;
    }

    /*** RESULT CLASS ***/
    public static class SimulationResult {
        public List<ScheduledAppointment> schedule;
        public long executionTimeMs;
        public int totalRequests;
        public int scheduledCount;
        public GeneticAlgorithmScheduler.GAMetrics metrics;
        public List<DoctorAvailability> doctors;

        public double getSuccessRate() {
            return totalRequests==0?0:(scheduledCount/(double)totalRequests)*100;
        }

        public Map<String,List<ScheduledAppointment>> getScheduleByDoctor() {
            Map<String,List<ScheduledAppointment>> grouped = new LinkedHashMap<>();
            for (ScheduledAppointment appt: schedule){
                String doctor = appt.getDoctor().getDoctorName();
                grouped.putIfAbsent(doctor,new ArrayList<>());
                grouped.get(doctor).add(appt);
            }
            for(List<ScheduledAppointment> appts: grouped.values()){
                appts.sort(Comparator.comparing(ScheduledAppointment::getDay)
                        .thenComparing(ScheduledAppointment::getStartTimeMinutes));
            }
            return grouped;
        }
    }
}
