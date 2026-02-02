package com.example.sttherese.doctor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.ga.*;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GASimulationActivity extends AppCompatActivity {

    private static final String TAG = "GASimulation";
    private MaterialAutoCompleteTextView spinnerScenario;

    private Button btnRunSimulation;
    private ProgressBar progressBar;
    private TextView tvResults;
    private RecyclerView recyclerViewSchedule;
    private ScheduleAdapter adapter;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ga_simulation);

        mainHandler = new Handler(Looper.getMainLooper());

        spinnerScenario = findViewById(R.id.spinnerScenario);
        btnRunSimulation = findViewById(R.id.btnRunSimulation);
        progressBar = findViewById(R.id.progressBar);
        tvResults = findViewById(R.id.tvResults);
        recyclerViewSchedule = findViewById(R.id.recyclerViewSchedule);

        adapter = new ScheduleAdapter();
        recyclerViewSchedule.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSchedule.setAdapter(adapter);

        // Setup spinner
        ArrayAdapter<String> scenarioAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Scenario 1: Basic", "Scenario 2: Patient Preferences",
                        "Scenario 3: High Demand", "Scenario 4: Edge Cases"});
        scenarioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerScenario.setAdapter(scenarioAdapter);

        btnRunSimulation.setOnClickListener(v -> runSimulation());

    }

    private void runSimulation() {
        btnRunSimulation.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvResults.setText("Running simulation... Please wait.");

        // Get the selected text from the dropdown
        String selectedScenario = spinnerScenario.getText().toString().trim();
        if (selectedScenario.isEmpty()) {
            Toast.makeText(this, "Please select a scenario", Toast.LENGTH_SHORT).show();
            btnRunSimulation.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            return;
        }

        new Thread(() -> {
            try {
                GASimulationRunner.SimulationResult result;
                GASimulationRunner.Scenario scenario;

                // Map selected text to enum
                switch (selectedScenario) {
                    case "Scenario 1: Basic":
                        scenario = GASimulationRunner.Scenario.BASIC;
                        break;
                    case "Scenario 2: Patient Preferences":
                        scenario = GASimulationRunner.Scenario.PATIENT_PREFERENCES;
                        break;
                    case "Scenario 3: High Demand":
                        scenario = GASimulationRunner.Scenario.HIGH_DEMAND;
                        break;
                    case "Scenario 4: Edge Cases":
                        scenario = GASimulationRunner.Scenario.EDGE_CASES;
                        break;
                    default:
                        mainHandler.post(() -> {
                            Toast.makeText(this, "Invalid scenario selected", Toast.LENGTH_SHORT).show();
                            btnRunSimulation.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                        });
                        return;
                }

                // Run the GA simulation
                result = GASimulationRunner.runSimulation(scenario);

                // Update UI on main thread
                mainHandler.post(() -> {
                    displayResults(result);
                    btnRunSimulation.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error running simulation", e);
                mainHandler.post(() -> {
                    tvResults.setText("ERROR: " + e.getMessage());
                    Toast.makeText(this, "Simulation failed", Toast.LENGTH_LONG).show();
                    btnRunSimulation.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }



    private void displayResults(GASimulationRunner.SimulationResult result) {
        try {
            // Build summary text
            StringBuilder summary = new StringBuilder();
            summary.append("📊 GA SIMULATION RESULTS\n\n");
            summary.append("✅ Scheduled: ").append(result.scheduledCount)
                    .append("/").append(result.totalRequests).append("\n");
            summary.append("📈 Success Rate: ").append(String.format("%.1f%%", result.getSuccessRate())).append("\n");
            summary.append("⏱️ Time: ").append(result.executionTimeMs).append(" ms\n");
            summary.append("🎯 Best Fitness: ").append(String.format("%.2f", result.metrics.bestFitness)).append("\n\n");

            // Validation status
//            summary.append("✅ Validation Result: ").append(result.metrics.isValid ? "VALID" : "INVALID").append("\n\n");

            // Doctor workload
            summary.append("👨‍⚕️ Doctor Workload Distribution:\n");
            Map<String, List<ScheduledAppointment>> byDoctor = result.getScheduleByDoctor();
            for (Map.Entry<String, List<ScheduledAppointment>> entry : byDoctor.entrySet()) {
                summary.append("  • ").append(entry.getKey()).append(": ")
                        .append(entry.getValue().size()).append(" appointments\n");
            }
            summary.append("\n");

            tvResults.setText(summary.toString());

            // Update RecyclerView with full schedule, sorted by doctor and time
            List<ScheduledAppointment> sortedSchedule = new ArrayList<>(result.schedule);
            sortedSchedule.sort(Comparator
                    .comparing((ScheduledAppointment a) -> a.getDoctor().getDoctorName())
                    .thenComparing(ScheduledAppointment::getDay)
                    .thenComparing(ScheduledAppointment::getStartTimeMinutes));

            adapter.setSchedule(sortedSchedule);

            Toast.makeText(this, "Simulation Complete!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error displaying results", e);
            tvResults.setText("Error displaying results: " + e.getMessage());
        }
    }


    // RecyclerView Adapter/Holder (same as before)
    private static class ScheduleAdapter extends RecyclerView.Adapter<ScheduleViewHolder> {
        private List<ScheduledAppointment> schedule = new ArrayList<>();

        public void setSchedule(List<ScheduledAppointment> schedule) {
            this.schedule = new ArrayList<>(schedule);
            notifyDataSetChanged();
        }

        @Override
        public ScheduleViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ScheduleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ScheduleViewHolder holder, int position) {
            ScheduledAppointment appt = schedule.get(position);
            String title = String.format("Patient %d → %s",
                    appt.getPatientId(), appt.getDoctor().getDoctorName());
            String subtitle = String.format("%s | %s - %s (%d min)",
                    appt.getDay(), formatTime(appt.getStartTimeMinutes()),
                    formatTime(appt.getEndTimeMinutes()),
                    appt.getEndTimeMinutes() - appt.getStartTimeMinutes());
            holder.bind(title, subtitle);
        }

        @Override
        public int getItemCount() {
            return schedule.size();
        }

        private static String formatTime(int minutes) {
            int hour = minutes / 60;
            int min = minutes % 60;
            String period = hour < 12 ? "AM" : "PM";
            int displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
            return String.format("%d:%02d %s", displayHour, min, period);
        }
    }

    private static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        public ScheduleViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
        public void bind(String title, String subtitle) {
            text1.setText(title);
            text2.setText(subtitle);
        }
    }
}