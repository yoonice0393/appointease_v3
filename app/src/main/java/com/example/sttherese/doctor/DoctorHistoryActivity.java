package com.example.sttherese.doctor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.adapters.DoctorHistoryAdapter;
import com.example.sttherese.adapters.HistoryAdapter;
import com.example.sttherese.adapters.OnItemCountChangeListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class DoctorHistoryActivity extends AppCompatActivity implements OnItemCountChangeListener {

    private static final String TAG = "DoctorHistoryActivity";
    private FirebaseFirestore db;
    private String userDocId; // This holds the Doctor's Document ID

    // UI Views
    private RecyclerView rvHistoryAppointments;
    private CardView cardEmptyHistory;

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
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(currentLimit);

        // Stop previous listener if adapter exists
        if (historyAdapter != null) {
            historyAdapter.stopListening();
        }

        // Initialize Adapter with the new query
        historyAdapter = new DoctorHistoryAdapter(historyQuery, this);

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
            this.currentStatusFilter = status.toLowerCase();
            Log.d(TAG, "Filter set to: " + this.currentStatusFilter);
        }

        // Reset limit when changing filters
        currentLimit = PAGE_SIZE;

        // Re-query with new filter
        setupHistoryList();
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