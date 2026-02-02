package com.example.sttherese.ga;

/**
 * Configuration class for GA Scheduler
 * Allows easy tuning of algorithm parameters
 */
public class SchedulerConfig {
    // Genetic Algorithm Parameters
    private int populationSize = 50;
    private int generations = 100;
    private double mutationRate = 0.15;
    private double crossoverRate = 0.7;
    private int tournamentSize = 5;
    private double elitismRate = 0.1; // Top 10% preserved

    // Fitness Weights
    private double weightOverlap = 200.0;
    private double weightSpecialtyMismatch = 100.0;
    private double weightInvalidDay = 50.0;
    private double weightInvalidSlot = 80.0;
    private double weightWorkloadImbalance = 10.0;
    private double weightPreferredDayMismatch = 20.0;
    private double weightPreferredTimeMismatch = 15.0;
    private double bonusMorningSlot = 5.0;
    private double bonusPreferredTime = 15.0;
    private double bonusPriorityMultiplier = 10.0;

    // Constraint Settings
    private int minBreakBetweenAppointments = 0; // minutes
    private int maxAppointmentsPerDoctorPerDay = 20;
    private boolean allowOverbooking = false;
    private boolean strictSpecialtyMatching = true;

    // Logging Settings
    private boolean verboseLogging = false;
    private int logInterval = 10; // Log every N generations
    private boolean enableMetricsTracking = true;

    // Convergence Settings
    private boolean enableEarlyStopping = true;
    private int earlyStoppingGenerations = 20; // Stop if no improvement
    private double convergenceThreshold = 0.01; // 1% improvement threshold

    // Builder pattern for easy configuration
    public static class Builder {
        private SchedulerConfig config = new SchedulerConfig();

        public Builder populationSize(int size) {
            config.populationSize = size;
            return this;
        }

        public Builder generations(int gen) {
            config.generations = gen;
            return this;
        }

        public Builder mutationRate(double rate) {
            config.mutationRate = rate;
            return this;
        }

        public Builder crossoverRate(double rate) {
            config.crossoverRate = rate;
            return this;
        }

        public Builder tournamentSize(int size) {
            config.tournamentSize = size;
            return this;
        }

        public Builder elitismRate(double rate) {
            config.elitismRate = rate;
            return this;
        }

        public Builder weightOverlap(double weight) {
            config.weightOverlap = weight;
            return this;
        }

        public Builder weightSpecialtyMismatch(double weight) {
            config.weightSpecialtyMismatch = weight;
            return this;
        }

        public Builder weightInvalidDay(double weight) {
            config.weightInvalidDay = weight;
            return this;
        }

        public Builder weightInvalidSlot(double weight) {
            config.weightInvalidSlot = weight;
            return this;
        }

        public Builder weightWorkloadImbalance(double weight) {
            config.weightWorkloadImbalance = weight;
            return this;
        }

        public Builder bonusMorningSlot(double bonus) {
            config.bonusMorningSlot = bonus;
            return this;
        }

        public Builder bonusPreferredTime(double bonus) {
            config.bonusPreferredTime = bonus;
            return this;
        }

        public Builder minBreakBetweenAppointments(int minutes) {
            config.minBreakBetweenAppointments = minutes;
            return this;
        }

        public Builder maxAppointmentsPerDoctorPerDay(int max) {
            config.maxAppointmentsPerDoctorPerDay = max;
            return this;
        }

        public Builder allowOverbooking(boolean allow) {
            config.allowOverbooking = allow;
            return this;
        }

        public Builder strictSpecialtyMatching(boolean strict) {
            config.strictSpecialtyMatching = strict;
            return this;
        }

        public Builder verboseLogging(boolean verbose) {
            config.verboseLogging = verbose;
            return this;
        }

        public Builder logInterval(int interval) {
            config.logInterval = interval;
            return this;
        }

        public Builder enableMetricsTracking(boolean enable) {
            config.enableMetricsTracking = enable;
            return this;
        }

        public Builder enableEarlyStopping(boolean enable) {
            config.enableEarlyStopping = enable;
            return this;
        }

        public Builder earlyStoppingGenerations(int generations) {
            config.earlyStoppingGenerations = generations;
            return this;
        }

        public SchedulerConfig build() {
            return config;
        }
    }

    // Predefined configurations
    public static SchedulerConfig getDefaultConfig() {
        return new Builder().build();
    }

    public static SchedulerConfig getFastConfig() {
        return new Builder()
                .populationSize(30)
                .generations(50)
                .mutationRate(0.2)
                .logInterval(5)
                .build();
    }

    public static SchedulerConfig getAccurateConfig() {
        return new Builder()
                .populationSize(100)
                .generations(200)
                .mutationRate(0.1)
                .crossoverRate(0.8)
                .tournamentSize(7)
                .build();
    }

    public static SchedulerConfig getBalancedConfig() {
        return new Builder()
                .populationSize(50)
                .generations(100)
                .mutationRate(0.15)
                .crossoverRate(0.7)
                .weightWorkloadImbalance(20.0)
                .build();
    }

    // Getters
    public int getPopulationSize() { return populationSize; }
    public int getGenerations() { return generations; }
    public double getMutationRate() { return mutationRate; }
    public double getCrossoverRate() { return crossoverRate; }
    public int getTournamentSize() { return tournamentSize; }
    public double getElitismRate() { return elitismRate; }

    public double getWeightOverlap() { return weightOverlap; }
    public double getWeightSpecialtyMismatch() { return weightSpecialtyMismatch; }
    public double getWeightInvalidDay() { return weightInvalidDay; }
    public double getWeightInvalidSlot() { return weightInvalidSlot; }
    public double getWeightWorkloadImbalance() { return weightWorkloadImbalance; }
    public double getWeightPreferredDayMismatch() { return weightPreferredDayMismatch; }
    public double getWeightPreferredTimeMismatch() { return weightPreferredTimeMismatch; }
    public double getBonusMorningSlot() { return bonusMorningSlot; }
    public double getBonusPreferredTime() { return bonusPreferredTime; }
    public double getBonusPriorityMultiplier() { return bonusPriorityMultiplier; }

    public int getMinBreakBetweenAppointments() { return minBreakBetweenAppointments; }
    public int getMaxAppointmentsPerDoctorPerDay() { return maxAppointmentsPerDoctorPerDay; }
    public boolean isAllowOverbooking() { return allowOverbooking; }
    public boolean isStrictSpecialtyMatching() { return strictSpecialtyMatching; }

    public boolean isVerboseLogging() { return verboseLogging; }
    public int getLogInterval() { return logInterval; }
    public boolean isEnableMetricsTracking() { return enableMetricsTracking; }

    public boolean isEnableEarlyStopping() { return enableEarlyStopping; }
    public int getEarlyStoppingGenerations() { return earlyStoppingGenerations; }
    public double getConvergenceThreshold() { return convergenceThreshold; }

    @Override
    public String toString() {
        return String.format(
                "SchedulerConfig{pop=%d, gen=%d, mut=%.2f, cross=%.2f, tournament=%d}",
                populationSize, generations, mutationRate, crossoverRate, tournamentSize
        );
    }
}