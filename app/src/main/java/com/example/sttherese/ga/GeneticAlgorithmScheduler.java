package com.example.sttherese.ga;

import java.util.*;
import java.util.stream.Collectors;

public class GeneticAlgorithmScheduler {
    private List<DoctorAvailability> doctors;
    private List<AppointmentRequest> requests;
    private Random random = new Random();

    // GA Parameters
    private static final int POPULATION_SIZE = 50;
    private static final int GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.15;
    private static final double CROSSOVER_RATE = 0.7;

    // Fitness weights
    private static final double WEIGHT_OVERLAP = 200.0;
    private static final double WEIGHT_SPECIALTY_MISMATCH = 100.0;
    private static final double WEIGHT_INVALID_DAY = 50.0;
    private static final double WEIGHT_INVALID_SLOT = 80.0;
    private static final double WEIGHT_WORKLOAD_IMBALANCE = 10.0;
    private static final double BONUS_MORNING_SLOT = 5.0;
    private static final double BONUS_PREFERRED_TIME = 15.0;

    // Metrics
    private GAMetrics metrics;

    public GeneticAlgorithmScheduler(List<DoctorAvailability> doctors,
                                             List<AppointmentRequest> requests) {
        this.doctors = doctors;
        this.requests = requests;
        this.metrics = new GAMetrics();
    }

    // Chromosome: represents a complete schedule
    private class Chromosome {
        List<Gene> genes;
        double fitness;
        boolean isValid;

        Chromosome() {
            genes = new ArrayList<>();
            fitness = 0.0;
            isValid = false;
        }

        Chromosome(List<Gene> genes) {
            this.genes = new ArrayList<>(genes);
            this.fitness = 0.0;
            this.isValid = false;
        }
    }

    // Gene: represents one appointment assignment
    private class Gene {
        int requestIndex;
        int doctorIndex;
        String day;
        int startTime;

        Gene(int requestIndex, int doctorIndex, String day, int startTime) {
            this.requestIndex = requestIndex;
            this.doctorIndex = doctorIndex;
            this.day = day;
            this.startTime = startTime;
        }

        Gene copy() {
            return new Gene(requestIndex, doctorIndex, day, startTime);
        }
    }

    // Metrics tracking class
    public static class GAMetrics {
        public double bestFitness = 0;
        public double avgFitness = 0;
        public int generation = 0;
        public int validSchedules = 0;
        public int totalConflicts = 0;
        public Map<Integer, Integer> doctorWorkload = new HashMap<>();
        public List<Double> fitnessHistory = new ArrayList<>();

        @Override
        public String toString() {
            return String.format(
                    "Generation: %d | Best Fitness: %.2f | Avg Fitness: %.2f | Valid Schedules: %d | Conflicts: %d",
                    generation, bestFitness, avgFitness, validSchedules, totalConflicts
            );
        }
    }

    // Validation result class
    public static class ValidationResult {
        public boolean isValid;
        public List<String> errors;
        public List<String> warnings;

        public ValidationResult() {
            this.isValid = true;
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
        }

        public void addError(String error) {
            this.errors.add(error);
            this.isValid = false;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Validation Result: ").append(isValid ? "VALID" : "INVALID").append("\n");
            if (!errors.isEmpty()) {
                sb.append("Errors:\n");
                errors.forEach(e -> sb.append("  - ").append(e).append("\n"));
            }
            if (!warnings.isEmpty()) {
                sb.append("Warnings:\n");
                warnings.forEach(w -> sb.append("  - ").append(w).append("\n"));
            }
            return sb.toString();
        }
    }

    // Main GA execution with logging
    public List<ScheduledAppointment> runSimulation() {
        System.out.println("=== Starting Genetic Algorithm Simulation ===");
        System.out.println("Population Size: " + POPULATION_SIZE);
        System.out.println("Generations: " + GENERATIONS);
        System.out.println("Requests: " + requests.size());
        System.out.println("Doctors: " + doctors.size());
        System.out.println();

        // Validate input data
        ValidationResult inputValidation = validateInputData();
        if (!inputValidation.isValid) {
            System.err.println("Input validation failed:");
            System.err.println(inputValidation);
            return new ArrayList<>();
        }

        // Initialize population
        List<Chromosome> population = initializePopulation();
        System.out.println("Initial population created\n");

        Chromosome bestOverall = null;

        // Evolve population
        for (int gen = 0; gen < GENERATIONS; gen++) {
            metrics.generation = gen;

            // Evaluate fitness
            double totalFitness = 0;
            int validCount = 0;

            for (Chromosome chromosome : population) {
                chromosome.fitness = calculateFitness(chromosome);
                totalFitness += chromosome.fitness;
                if (chromosome.isValid) validCount++;
            }

            metrics.avgFitness = totalFitness / population.size();
            metrics.validSchedules = validCount;

            // Sort by fitness (higher is better)
            population.sort((c1, c2) -> Double.compare(c2.fitness, c1.fitness));

            Chromosome currentBest = population.get(0);
            metrics.bestFitness = currentBest.fitness;
            metrics.fitnessHistory.add(currentBest.fitness);

            if (bestOverall == null || currentBest.fitness > bestOverall.fitness) {
                bestOverall = new Chromosome(currentBest.genes);
                bestOverall.fitness = currentBest.fitness;
                bestOverall.isValid = currentBest.isValid;
            }

            // Log progress every 10 generations
            if (gen % 10 == 0 || gen == GENERATIONS - 1) {
                System.out.println(metrics);
            }

            // Early stopping if perfect solution found
            if (currentBest.isValid && metrics.totalConflicts == 0) {
                System.out.println("\nPerfect solution found at generation " + gen);
                break;
            }

            // Create next generation
            List<Chromosome> nextGeneration = new ArrayList<>();

            // Elitism: keep top 10%
            int eliteCount = POPULATION_SIZE / 10;
            for (int i = 0; i < eliteCount; i++) {
                nextGeneration.add(population.get(i));
            }

            // Fill rest with offspring
            while (nextGeneration.size() < POPULATION_SIZE) {
                Chromosome parent1 = tournamentSelection(population);
                Chromosome parent2 = tournamentSelection(population);

                Chromosome offspring;
                if (random.nextDouble() < CROSSOVER_RATE) {
                    offspring = crossover(parent1, parent2);
                } else {
                    offspring = new Chromosome(parent1.genes);
                }

                if (random.nextDouble() < MUTATION_RATE) {
                    mutate(offspring);
                }

                nextGeneration.add(offspring);
            }

            population = nextGeneration;
        }

        System.out.println("\n=== Evolution Complete ===");
        System.out.println("Best fitness achieved: " + bestOverall.fitness);

        // Convert and validate final schedule
        List<ScheduledAppointment> schedule = convertToSchedule(bestOverall);
        ValidationResult finalValidation = validateSchedule(schedule);

        System.out.println("\n" + finalValidation);

        // Print workload distribution
        printWorkloadDistribution(schedule);

        return schedule;
    }

    // Validate input data
    private ValidationResult validateInputData() {
        ValidationResult result = new ValidationResult();

        if (doctors == null || doctors.isEmpty()) {
            result.addError("No doctors available");
        }

        if (requests == null || requests.isEmpty()) {
            result.addError("No appointment requests provided");
        }

        // Check if each request has at least one eligible doctor
        for (AppointmentRequest req : requests) {
            boolean hasEligibleDoctor = false;
            for (DoctorAvailability doc : doctors) {
                if (doc.getSpecialty().equals(req.getSpecialty())) {
                    hasEligibleDoctor = true;
                    break;
                }
            }
            if (!hasEligibleDoctor) {
                result.addError("No doctor available for specialty: " + req.getSpecialty() +
                        " (Patient " + req.getPatientId() + ")");
            }
        }

        // Validate doctor data
        for (DoctorAvailability doc : doctors) {
            if (doc.getWorkingDays() == null || doc.getWorkingDays().isEmpty()) {
                result.addWarning("Doctor " + doc.getDoctorName() + " has no working days");
            }
            if (doc.getAvailableSlots() == null || doc.getAvailableSlots().isEmpty()) {
                result.addWarning("Doctor " + doc.getDoctorName() + " has no available slots");
            }
        }

        return result;
    }

    // Validate final schedule
    private ValidationResult validateSchedule(List<ScheduledAppointment> schedule) {
        ValidationResult result = new ValidationResult();
        Map<String, List<ScheduledAppointment>> doctorDaySchedule = new HashMap<>();

        for (ScheduledAppointment appt : schedule) {
            DoctorAvailability doctor = appt.getDoctor();

            // Check if day is valid
            if (!doctor.getWorkingDays().contains(appt.getDay())) {
                result.addError("Appointment scheduled on invalid day: " + appt);
            }

            // Check if time is within available slots
            boolean withinSlot = false;
            for (TimeRange slot : doctor.getAvailableSlots()) {
                if (appt.getStartTimeMinutes() >= slot.getStartMinutes() &&
                        appt.getEndTimeMinutes() <= slot.getEndMinutes()) {
                    withinSlot = true;
                    break;
                }
            }
            if (!withinSlot) {
                result.addError("Appointment outside available slots: " + appt);
            }

            // Check for overlaps
            String key = doctor.getDoctorId() + "_" + appt.getDay();
            doctorDaySchedule.putIfAbsent(key, new ArrayList<>());

            for (ScheduledAppointment existing : doctorDaySchedule.get(key)) {
                if (!(appt.getEndTimeMinutes() <= existing.getStartTimeMinutes() ||
                        appt.getStartTimeMinutes() >= existing.getEndTimeMinutes())) {
                    result.addError("Overlapping appointments: " + appt + " and " + existing);
                }
            }

            doctorDaySchedule.get(key).add(appt);
        }

        // Check if all requests were scheduled
        Set<Integer> scheduledPatients = schedule.stream()
                .map(ScheduledAppointment::getPatientId)
                .collect(Collectors.toSet());

        for (AppointmentRequest req : requests) {
            if (!scheduledPatients.contains(req.getPatientId())) {
                result.addWarning("Patient " + req.getPatientId() + " was not scheduled");
            }
        }

        return result;
    }

    // Print workload distribution
    private void printWorkloadDistribution(List<ScheduledAppointment> schedule) {
        Map<String, Integer> workload = new HashMap<>();

        for (ScheduledAppointment appt : schedule) {
            String doctorName = appt.getDoctor().getDoctorName();
            workload.put(doctorName, workload.getOrDefault(doctorName, 0) + 1);
        }

        System.out.println("\n=== Doctor Workload Distribution ===");
        workload.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue() + " appointments"));
    }

    // Initialize random population
    private List<Chromosome> initializePopulation() {
        List<Chromosome> population = new ArrayList<>();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            Chromosome chromosome = new Chromosome();

            for (int reqIdx = 0; reqIdx < requests.size(); reqIdx++) {
                AppointmentRequest req = requests.get(reqIdx);

                // Find eligible doctors
                List<Integer> eligibleDoctorIndices = new ArrayList<>();
                for (int docIdx = 0; docIdx < doctors.size(); docIdx++) {
                    if (doctors.get(docIdx).getSpecialty().equals(req.getSpecialty())) {
                        eligibleDoctorIndices.add(docIdx);
                    }
                }

                if (eligibleDoctorIndices.isEmpty()) {
                    System.err.println("Warning: No eligible doctor for request " + reqIdx);
                    continue;
                }

                // Random assignment
                int doctorIndex = eligibleDoctorIndices.get(random.nextInt(eligibleDoctorIndices.size()));
                DoctorAvailability doctor = doctors.get(doctorIndex);

                if (doctor.getWorkingDays().isEmpty() || doctor.getAvailableSlots().isEmpty()) {
                    continue;
                }

                String day = doctor.getWorkingDays().get(random.nextInt(doctor.getWorkingDays().size()));
                TimeRange slot = doctor.getAvailableSlots().get(random.nextInt(doctor.getAvailableSlots().size()));

                int maxStart = Math.max(slot.getStartMinutes(),
                        slot.getEndMinutes() - req.getDurationMinutes());
                int startTime = slot.getStartMinutes();
                if (maxStart > slot.getStartMinutes()) {
                    startTime += random.nextInt(maxStart - slot.getStartMinutes() + 1);
                }

                chromosome.genes.add(new Gene(reqIdx, doctorIndex, day, startTime));
            }

            population.add(chromosome);
        }

        return population;
    }

    // Calculate fitness with workload balancing
    private double calculateFitness(Chromosome chromosome) {
        double fitness = 1000.0; // Start with base score

        Map<String, List<Gene>> doctorDaySchedule = new HashMap<>();
        Map<Integer, Integer> doctorAppointmentCount = new HashMap<>();
        int conflictCount = 0;

        for (Gene gene : chromosome.genes) {
            AppointmentRequest req = requests.get(gene.requestIndex);
            DoctorAvailability doctor = doctors.get(gene.doctorIndex);

            // Track workload
            doctorAppointmentCount.put(gene.doctorIndex,
                    doctorAppointmentCount.getOrDefault(gene.doctorIndex, 0) + 1);

            // Penalty if specialty doesn't match
            if (!doctor.getSpecialty().equals(req.getSpecialty())) {
                fitness -= WEIGHT_SPECIALTY_MISMATCH;
                continue;
            }

            // Penalty if day not available
            if (!doctor.getWorkingDays().contains(gene.day)) {
                fitness -= WEIGHT_INVALID_DAY;
                continue;
            }

            // Penalty if time outside available slots
            boolean withinSlot = false;
            for (TimeRange slot : doctor.getAvailableSlots()) {
                if (gene.startTime >= slot.getStartMinutes() &&
                        gene.startTime + req.getDurationMinutes() <= slot.getEndMinutes()) {
                    withinSlot = true;
                    break;
                }
            }
            if (!withinSlot) {
                fitness -= WEIGHT_INVALID_SLOT;
                continue;
            }

            // Check for overlaps with same doctor on same day
            String key = gene.doctorIndex + "_" + gene.day;
            doctorDaySchedule.putIfAbsent(key, new ArrayList<>());

            for (Gene existing : doctorDaySchedule.get(key)) {
                AppointmentRequest existingReq = requests.get(existing.requestIndex);
                int existingEnd = existing.startTime + existingReq.getDurationMinutes();
                int currentEnd = gene.startTime + req.getDurationMinutes();

                // Check overlap
                if (!(currentEnd <= existing.startTime || gene.startTime >= existingEnd)) {
                    fitness -= WEIGHT_OVERLAP;
                    conflictCount++;
                }
            }

            doctorDaySchedule.get(key).add(gene);

            // Bonus for morning slots (before 10 AM)
            if (gene.startTime < 600) {
                fitness += BONUS_MORNING_SLOT;
            }

            // Bonus for preferred time (9 AM - 11 AM)
            if (gene.startTime >= 540 && gene.startTime < 660) {
                fitness += BONUS_PREFERRED_TIME;
            }
        }

        // Workload balancing: penalize imbalanced workloads
        if (!doctorAppointmentCount.isEmpty()) {
            double avgWorkload = doctorAppointmentCount.values().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0);

            for (int count : doctorAppointmentCount.values()) {
                double deviation = Math.abs(count - avgWorkload);
                fitness -= deviation * WEIGHT_WORKLOAD_IMBALANCE;
            }
        }

        metrics.totalConflicts = conflictCount;
        chromosome.isValid = (conflictCount == 0 && fitness > 500);

        return Math.max(0, fitness);
    }

    // Tournament selection
    private Chromosome tournamentSelection(List<Chromosome> population) {
        int tournamentSize = 5;
        Chromosome best = null;

        for (int i = 0; i < tournamentSize; i++) {
            Chromosome candidate = population.get(random.nextInt(population.size()));
            if (best == null || candidate.fitness > best.fitness) {
                best = candidate;
            }
        }

        return best;
    }

    // Single-point crossover
    private Chromosome crossover(Chromosome parent1, Chromosome parent2) {
        int size = Math.min(parent1.genes.size(), parent2.genes.size());
        if (size == 0) return new Chromosome();

        int crossoverPoint = random.nextInt(size);

        Chromosome offspring = new Chromosome();

        for (int i = 0; i < size; i++) {
            if (i < crossoverPoint) {
                offspring.genes.add(parent1.genes.get(i).copy());
            } else {
                offspring.genes.add(parent2.genes.get(i).copy());
            }
        }

        return offspring;
    }

    // Mutation: randomly change one gene
    private void mutate(Chromosome chromosome) {
        if (chromosome.genes.isEmpty()) return;

        int geneIndex = random.nextInt(chromosome.genes.size());
        Gene gene = chromosome.genes.get(geneIndex);
        AppointmentRequest req = requests.get(gene.requestIndex);

        // Find eligible doctors
        List<Integer> eligibleDoctorIndices = new ArrayList<>();
        for (int i = 0; i < doctors.size(); i++) {
            if (doctors.get(i).getSpecialty().equals(req.getSpecialty())) {
                eligibleDoctorIndices.add(i);
            }
        }

        if (eligibleDoctorIndices.isEmpty()) return;

        // Mutate to random valid values
        gene.doctorIndex = eligibleDoctorIndices.get(random.nextInt(eligibleDoctorIndices.size()));
        DoctorAvailability doctor = doctors.get(gene.doctorIndex);

        if (doctor.getWorkingDays().isEmpty() || doctor.getAvailableSlots().isEmpty()) {
            return;
        }

        gene.day = doctor.getWorkingDays().get(random.nextInt(doctor.getWorkingDays().size()));

        TimeRange slot = doctor.getAvailableSlots().get(random.nextInt(doctor.getAvailableSlots().size()));
        int maxStart = Math.max(slot.getStartMinutes(),
                slot.getEndMinutes() - req.getDurationMinutes());
        gene.startTime = slot.getStartMinutes();
        if (maxStart > slot.getStartMinutes()) {
            gene.startTime += random.nextInt(maxStart - slot.getStartMinutes() + 1);
        }
    }

    // Convert chromosome to scheduled appointments
    private List<ScheduledAppointment> convertToSchedule(Chromosome chromosome) {
        List<ScheduledAppointment> schedule = new ArrayList<>();

        for (Gene gene : chromosome.genes) {
            AppointmentRequest req = requests.get(gene.requestIndex);
            DoctorAvailability doctor = doctors.get(gene.doctorIndex);

            schedule.add(new ScheduledAppointment(
                    req.getPatientId(),
                    doctor,
                    gene.day,
                    gene.startTime,
                    gene.startTime + req.getDurationMinutes()
            ));
        }

        return schedule;
    }

    // Get metrics for analysis
    public GAMetrics getMetrics() {
        return metrics;
    }
}