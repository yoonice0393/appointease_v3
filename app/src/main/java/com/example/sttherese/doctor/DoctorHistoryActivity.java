package com.example.sttherese.doctor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.DateFormatter;
import com.example.sttherese.R;
import com.example.sttherese.adapters.DoctorHistoryAdapter;
import com.example.sttherese.adapters.OnItemCountChangeListener;
import com.example.sttherese.models.Appointment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DoctorHistoryActivity extends AppCompatActivity implements OnItemCountChangeListener {
    private static final String TAG = "DoctorHistoryActivity";
    private FirebaseFirestore db;
    private String userDocId; // This holds the Doctor's Document ID

    // UI Views
    private RecyclerView rvHistoryAppointments;
    private CardView cardEmptyHistory;
    private TextView tvNoHistoryTitle, tvNoHistoryMessage;

    // Adapter
    private DoctorHistoryAdapter historyAdapter;

    // Bottom Navigation
    private LinearLayout btnHome, btnCalendar, btnHistory;
    private ImageView btnAdd;
    private LinearLayout btnAppointment;
    private String currentStatusFilter = null;
    private static final int PAGE_SIZE = 6;
    private int currentLimit = PAGE_SIZE;
    private ChipGroup chipGroupStatusFilters;
    private String doctorQueryId; // Will hold the short ID (e.g., D001)

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_history);

        initializeViews();
        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userDocId = prefs.getString("user_doc_id", null);
        Log.d(TAG, "Retrieved Doctor ID: " + userDocId);

        // Initialize with "All" filter (null means no filter)
        currentStatusFilter = null;

        setupFilterListener();
        setupClickListeners();

        if (userDocId != null) {
            fetchDoctorQueryId(userDocId);
        } else {
            Toast.makeText(this, "Doctor not logged in.", Toast.LENGTH_SHORT).show();
            cardEmptyHistory.setVisibility(View.VISIBLE);
            rvHistoryAppointments.setVisibility(View.GONE);
        }
    }

    private void setupFilterListener() {
        chipGroupStatusFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String status;

            if (checkedIds.isEmpty()) {
                // Default to "All" if nothing is selected
                status = "All";
                // Re-check the "All" chip programmatically
                chipGroupStatusFilters.check(R.id.chipAll);
            } else {
                int checkedId = checkedIds.get(0);
                Chip checkedChip = findViewById(checkedId);
                status = (checkedChip != null) ? checkedChip.getText().toString() : "All";
            }

            Log.d(TAG, "Filter changed to: " + status);
            setStatusFilter(status);
        });
    }

    private void initializeViews() {
        // Bottom Navigation
        btnHome = findViewById(R.id.btnHome);
        btnAppointment = findViewById(R.id.btnAppointment);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnHistory = findViewById(R.id.btnHistory);
        btnAdd = findViewById(R.id.btnAdd);

        // History specific views
        rvHistoryAppointments = findViewById(R.id.rvHistoryAppointments);
        cardEmptyHistory = findViewById(R.id.cardEmptyHistory);
        chipGroupStatusFilters = findViewById(R.id.chipGroupStatusFilters);
        tvNoHistoryTitle = findViewById(R.id.tvNoHistoryTitle);
        tvNoHistoryMessage = findViewById(R.id.tvNoHistoryMessage);
    }

    private void setupClickListeners() {
        btnHome.setOnClickListener(v -> startActivity(new Intent(this, DoctorHomeActivity.class)));
        btnAppointment.setOnClickListener(v -> startActivity(new Intent(this, DoctorAppointmentActivity.class)));
        btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, DoctorCalendarActivity.class)));
        btnHistory.setOnClickListener(v -> Toast.makeText(this, "Already on History", Toast.LENGTH_SHORT).show());
        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, AvailabilityScheduling.class)));
    }

    private void fetchDoctorQueryId(String longUserId) {
        Log.d(TAG, "Fetching doctor query ID for user: " + longUserId);

        Query doctorLookupQuery = db.collection("doctors")
                .whereEqualTo("user_id", longUserId)
                .limit(1);

        doctorLookupQuery.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot doctorDoc = task.getResult().getDocuments().get(0);
                        doctorQueryId = doctorDoc.getId();

                        Log.d(TAG, "Fetched short Doctor ID (Query ID): " + doctorQueryId);

                        if (doctorQueryId != null) {
                            setupHistoryList();
                        } else {
                            Toast.makeText(this, "Internal Error: Could not get Doctor Document ID.", Toast.LENGTH_LONG).show();
                            cardEmptyHistory.setVisibility(View.VISIBLE);
                            rvHistoryAppointments.setVisibility(View.GONE);
                        }
                    } else {
                        Log.e(TAG, "Error or Doctor Profile Not Found using user_id: ", task.getException());
                        Toast.makeText(this, "Doctor Profile not linked to user ID.", Toast.LENGTH_SHORT).show();
                        cardEmptyHistory.setVisibility(View.VISIBLE);
                        rvHistoryAppointments.setVisibility(View.GONE);
                    }
                });
    }

    private void setupHistoryList() {
        if (doctorQueryId == null) {
            Log.e(TAG, "Doctor Query ID is null, cannot setup history list");
            cardEmptyHistory.setVisibility(View.VISIBLE);
            rvHistoryAppointments.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "Setting up history list with filter: " + currentStatusFilter + ", limit: " + currentLimit);

        // Start building the query
        Query historyQuery = db.collection("appointments")
                .whereEqualTo("doctorId", doctorQueryId);

        // Apply status filter if one is selected (not null/All)
        if (currentStatusFilter != null) {
            Log.d(TAG, "Applying status filter: " + currentStatusFilter);
            historyQuery = historyQuery.whereEqualTo("status", currentStatusFilter);
        } else {
            Log.d(TAG, "No status filter applied (showing all)");
        }

        // Add ordering and limit AFTER where clauses
        historyQuery = historyQuery
                .orderBy("date", Query.Direction.DESCENDING);

        // Stop previous listener if adapter exists
        if (historyAdapter != null) {
            historyAdapter.stopListening();
        }

        // Initialize Adapter with the new query
        historyAdapter = new DoctorHistoryAdapter(historyQuery, this, this::showAppointmentDetailDialog);

        rvHistoryAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvHistoryAppointments.setAdapter(historyAdapter);

        // Add error listener to catch index issues
        historyQuery.get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Query successful, found " + querySnapshot.size() + " documents");
                    historyAdapter.startListening();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Query failed - Check if you need to create a composite index!", e);
                    Toast.makeText(this, "Database index required. Check Logcat for link.", Toast.LENGTH_LONG).show();
                    cardEmptyHistory.setVisibility(View.VISIBLE);
                    rvHistoryAppointments.setVisibility(View.GONE);
                });

        Log.d(TAG, "History adapter setup complete");
    }

    public void loadMoreHistory() {
        currentLimit += PAGE_SIZE;
        Log.d(TAG, "Loading more history, new limit: " + currentLimit);
        setupHistoryList();
    }

    public void setStatusFilter(String status) {
        Log.d(TAG, "setStatusFilter called with: " + status);

        if (status == null || status.equalsIgnoreCase("All")) {
            this.currentStatusFilter = null;
            Log.d(TAG, "Filter set to null (show all)");
        } else {
            // Convert to lowercase to match Firestore values
            this.currentStatusFilter = normalizeStatus(status);
            Log.d(TAG, "Filter set to: " + this.currentStatusFilter);
        }

        // Reset limit when changing filters
        currentLimit = PAGE_SIZE;
        updateEmptyState(status);

        // Re-query with new filter
        setupHistoryList();
    }

    private void updateEmptyState(String filterStatus) {
        String normalized = normalizeStatus(filterStatus);
        switch (normalized) {
            case "approved":
                tvNoHistoryTitle.setText("NO APPROVED APPOINTMENTS");
                tvNoHistoryMessage.setText("You don't have any approved appointments assigned to you.");
                break;
            case "denied":
                tvNoHistoryTitle.setText("NO DENIED APPOINTMENTS");
                tvNoHistoryMessage.setText("You don't have any denied appointments assigned to you.");
                break;
            case "cancelled":
                tvNoHistoryTitle.setText("NO CANCELLED APPOINTMENTS");
                tvNoHistoryMessage.setText("You don't have any cancelled appointments assigned to you.");
                break;
            case "completed":
                tvNoHistoryTitle.setText("NO COMPLETED APPOINTMENTS");
                tvNoHistoryMessage.setText("You don't have any completed appointments assigned to you.");
                break;
            default:
                tvNoHistoryTitle.setText("NO HISTORY");
                tvNoHistoryMessage.setText("You don't have any appointment history assigned to you.");
                break;
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.toLowerCase(Locale.ROOT).trim();
        if (normalized.equals("canceled")) return "cancelled";
        return normalized;
    }

    private void showAppointmentDetailDialog(Appointment appointment, DocumentSnapshot snapshot) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_appointment_history_detail, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        TextView body = dialogView.findViewById(R.id.textAppointmentDetailBody);
        MaterialButton closeButton = dialogView.findViewById(R.id.buttonCloseAppointmentDetail);

        String status = valueOrFallback(appointment.getStatus(), snapshot, "status", "N/A");
        String details = "Appointment Type: " + valueOrFallback(appointment.getAppointmentType(), snapshot, "appointmentType", valueOrFallback(appointment.getSpecialty(), snapshot, "specialty", "N/A")) +
                "\nDate: " + formatDate(valueOrFallback(appointment.getDate(), snapshot, "date", "N/A")) +
                "\nTime: " + valueOrFallback(appointment.getTime(), snapshot, "time", "N/A") +
                "\nDoctor: " + valueOrFallback(appointment.getDoctorName(), snapshot, "doctorName", "You") +
                "\nPatient: " + valueOrFallback(appointment.getPatientName(), snapshot, "patientName", "Patient") +
                "\nStatus: " + capitalizeFirst(status);

        String statusLower = status.toLowerCase(Locale.ROOT);
        if (statusLower.contains("cancel") || statusLower.contains("denied")) {
            details += "\nReason: " + firstSnapshotValue(snapshot, "reason", "cancel_reason", "cancellation_reason", "cancelled_reason", "denial_reason", "denied_reason", "staff_reason");
        }
        if (statusLower.contains("completed")) {
            details += "\nDoctor Note: " + firstSnapshotValue(snapshot, "doctor_note", "doctor_notes", "consultation_note", "consultation_notes", "note", "notes");
        }

        body.setText(details);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private String valueOrFallback(String value, DocumentSnapshot snapshot, String field, String fallback) {
        if (value != null && !value.trim().isEmpty()) return value.trim();
        String snapValue = snapshot != null ? snapshot.getString(field) : null;
        return snapValue != null && !snapValue.trim().isEmpty() ? snapValue.trim() : fallback;
    }

    private String firstSnapshotValue(DocumentSnapshot snapshot, String... fields) {
        if (snapshot != null) {
            for (String field : fields) {
                Object value = snapshot.get(field);
                if (value != null && !value.toString().trim().isEmpty()) {
                    return value.toString().trim();
                }
            }
        }
        return "N/A";
    }

    private String formatDate(String dateString) {
        return DateFormatter.formatToFullDate(dateString);
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.trim().isEmpty()) return "N/A";
        String cleaned = text.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    @Override
    public void onLoadMore() {
        Log.d(TAG, "onLoadMore triggered by adapter. Calling loadMoreHistory().");
        loadMoreHistory();
    }

    @Override
    public void onCountChange(int count) {
        Log.d(TAG, "onCountChange called with count: " + count);

        if (count > 0) {
            rvHistoryAppointments.setVisibility(View.VISIBLE);
            cardEmptyHistory.setVisibility(View.GONE);
        } else {
            rvHistoryAppointments.setVisibility(View.GONE);
            cardEmptyHistory.setVisibility(View.VISIBLE);
            updateEmptyState(currentStatusFilter != null ? currentStatusFilter : "All");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
        // startListening is already called in setupHistoryList()
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
        if (historyAdapter != null) {
            historyAdapter.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        if (historyAdapter != null) {
            historyAdapter.stopListening();
        }
    }
}
