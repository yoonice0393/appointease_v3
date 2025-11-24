package com.example.sttherese.patient.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.adapters.HistoryAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.sttherese.adapters.OnItemCountChangeListener;

public class HistoryActivity extends AppCompatActivity implements
        OnItemCountChangeListener,
        HistoryAdapter.OnLoadMoreListener {

    private static final String TAG = "HistoryActivity";

    // Firestore
    private FirebaseFirestore db;
    private String userDocId;

    // UI Views
    private RecyclerView rvHistoryAppointments;
    private CardView cardEmptyHistory;
    private ChipGroup chipGroupStatusFilters;
    private TextView tvNoHistoryTitle;
    private TextView tvNoHistoryMessage;
    private ImageView ivEmptyIcon;

    // Adapter
    private HistoryAdapter historyAdapter;

    // Bottom Navigation
    private LinearLayout btnHome, btnDoctor, btnCalendar, btnHistory;
    private ImageView btnAdd;

    // Filtering and Pagination
    private String currentStatusFilter = null; // null for "All"
    private static final int PAGE_SIZE = 6;
    private int currentLimit = PAGE_SIZE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userDocId = prefs.getString("user_doc_id", null);
        Log.d(TAG, "Retrieved User ID: " + userDocId);

        // Initialize filtering state
        currentStatusFilter = null;
        currentLimit = PAGE_SIZE;

        initializeViews();
        setupFilterListener();
        setupClickListeners();

        if (userDocId != null) {
            setupHistoryList();
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            updateEmptyState("All");
            cardEmptyHistory.setVisibility(View.VISIBLE);
            rvHistoryAppointments.setVisibility(View.GONE);
        }
    }

    private void initializeViews() {
        // History specific views
        rvHistoryAppointments = findViewById(R.id.rvHistoryAppointments);
        cardEmptyHistory = findViewById(R.id.cardEmptyHistory);
        chipGroupStatusFilters = findViewById(R.id.chipGroupStatusFilters);
        tvNoHistoryTitle = findViewById(R.id.tvNoHistoryTitle);
        tvNoHistoryMessage = findViewById(R.id.tvNoHistoryMessage);
//        ivEmptyIcon = findViewById(R.id.ivEmptyIcon);

        rvHistoryAppointments.setLayoutManager(new LinearLayoutManager(this));

        // Bottom Navigation
        btnHome = findViewById(R.id.btnHome);
        btnDoctor = findViewById(R.id.btnDoctor);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnHistory = findViewById(R.id.btnHistory);
        btnAdd = findViewById(R.id.btnAdd);
    }

    // --- NEW METHOD: Update empty state based on filter ---
    private void updateEmptyState(String filterStatus) {
        switch (filterStatus.toLowerCase()) {
            case "pending":
                tvNoHistoryTitle.setText("NO PENDING APPOINTMENTS");
                tvNoHistoryMessage.setText("You don't have any pending appointments at the moment.");
                // Optional: Change icon color or drawable
                break;
            case "confirmed":
                tvNoHistoryTitle.setText("NO CONFIRMED APPOINTMENTS");
                tvNoHistoryMessage.setText("You don't have any confirmed appointments yet.");
                break;
            case "completed":
                tvNoHistoryTitle.setText("NO COMPLETED VISITS");
                tvNoHistoryMessage.setText("You haven't completed any visits to the center yet.");
                break;
            case "cancelled":
                tvNoHistoryTitle.setText("NO CANCELLED APPOINTMENTS");
                tvNoHistoryMessage.setText("You don't have any cancelled appointments.");
                break;
            case "all":
            default:
                tvNoHistoryTitle.setText("NO HISTORY");
                tvNoHistoryMessage.setText("You don't have any past visits in the center currently.");
                break;
        }
    }

    // --- Filtering Logic ---

    private void setupFilterListener() {
        chipGroupStatusFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String status;

            if (checkedIds.isEmpty()) {
                // Safety measure, but should not happen with singleSelection=true.
                // If it *did* happen, we default the logic to 'All'
                status = "All";


            } else {
                // Since singleSelection is true, checkedIds will have exactly one ID.
                int checkedId = checkedIds.get(0);
                Chip checkedChip = findViewById(checkedId);

                // Get the text directly, relying on the fact that a chip is selected.
                status = (checkedChip != null) ? checkedChip.getText().toString() : "All";
            }

            Log.d(TAG, "Filter changed to: " + status);
            setStatusFilter(status);
        });
    }

    public void setStatusFilter(String status) {
        Log.d(TAG, "setStatusFilter called with: " + status);

        if (status == null || status.equalsIgnoreCase("All")) {
            this.currentStatusFilter = null;
        } else {
            // Convert to lowercase to match Firestore values
            this.currentStatusFilter = status.toLowerCase();
        }

        // Update empty state message
        updateEmptyState(status);

        // Reset limit when changing filters
        currentLimit = PAGE_SIZE;

        // Re-query with new filter (this recreates the adapter)
        setupHistoryList();
    }

    public void loadMoreHistory() {
        // Prevent loading more if adapter indicates no more data
        if (historyAdapter != null && !historyAdapter.hasMoreData()) {
            Log.d(TAG, "No more data to load, skipping loadMoreHistory");
            return;
        }

        currentLimit += PAGE_SIZE;
        Log.d(TAG, "Loading more history, new limit: " + currentLimit);

        // Update the query without recreating the adapter
        updateAdapterQuery();
    }

    // --- Update query without recreating adapter ---
    private void updateAdapterQuery() {
        if (userDocId == null) return;

        Log.d(TAG, "Updating adapter query with filter: " + currentStatusFilter + ", limit: " + currentLimit);

        // Build the query
        Query historyQuery = db.collection("appointments")
                .whereEqualTo("userId", userDocId);

        if (currentStatusFilter != null) {
            historyQuery = historyQuery.whereEqualTo("status", currentStatusFilter);
        }

        historyQuery = historyQuery
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(currentLimit);

        // Update the existing adapter's query
        if (historyAdapter != null) {
            historyAdapter.updateQuery(historyQuery);

            // Check if there's more data
            historyQuery.get()
                    .addOnSuccessListener(querySnapshot -> {
                        Log.d(TAG, "Query successful, found " + querySnapshot.size() + " documents");
                        if (historyAdapter != null) {
                            historyAdapter.setHasMoreData(querySnapshot.size() >= currentLimit);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Query update failed", e);
                    });
        }
    }

    // --- Updated History List Setup (only for initial setup and filter changes) ---
    private void setupHistoryList() {
        if (userDocId == null) return;

        Log.d(TAG, "Setting up history list with filter: " + currentStatusFilter + ", limit: " + currentLimit);

        // Start building the query
        Query historyQuery = db.collection("appointments")
                .whereEqualTo("userId", userDocId);

        // Apply status filter if one is selected
        if (currentStatusFilter != null) {
            Log.d(TAG, "Applying status filter: " + currentStatusFilter);
            historyQuery = historyQuery.whereEqualTo("status", currentStatusFilter);
        } else {
            Log.d(TAG, "No status filter applied (showing all)");
        }

        // Add ordering and limit
        historyQuery = historyQuery
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(currentLimit);

        // Stop previous listener if adapter exists
        if (historyAdapter != null) {
            historyAdapter.stopListening();
        }

        // Initialize Adapter with the new query
        historyAdapter = new HistoryAdapter(historyQuery, this, this);

        rvHistoryAppointments.setAdapter(historyAdapter);

        // Execute query to check for composite index requirement
        historyQuery.get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Query successful, found " + querySnapshot.size() + " documents");

                    // Tell adapter if there are more items to load
                    if (historyAdapter != null) {
                        historyAdapter.setHasMoreData(querySnapshot.size() >= currentLimit);
                    }

                    historyAdapter.startListening();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Query failed - Check if you need to create a composite index!", e);
                    Toast.makeText(this, "Database index required. Check Logcat for link.", Toast.LENGTH_LONG).show();
                    cardEmptyHistory.setVisibility(View.VISIBLE);
                    rvHistoryAppointments.setVisibility(View.GONE);
                });
    }

    // --- OnItemCountChangeListener implementation ---
    @Override
    public void onCountChange(int count) {
        Log.d(TAG, "onCountChange called with count: " + count);
        if (count > 0) {
            rvHistoryAppointments.setVisibility(View.VISIBLE);
            cardEmptyHistory.setVisibility(View.GONE);
        } else {
            rvHistoryAppointments.setVisibility(View.GONE);
            cardEmptyHistory.setVisibility(View.VISIBLE);
            // Update the empty state message based on current filter
            String displayFilter = currentStatusFilter != null ? currentStatusFilter : "All";
            updateEmptyState(displayFilter);
        }
    }

    // --- HistoryAdapter.OnLoadMoreListener implementation ---
    @Override
    public void onLoadMore() {
        Log.d(TAG, "onLoadMore triggered by adapter. Calling loadMoreHistory().");
        loadMoreHistory();
    }

    private void setupClickListeners() {
        btnHome.setOnClickListener(v -> {
            showToast("Home");
            startActivity(new Intent(HistoryActivity.this, Home.class));
        });

        btnDoctor.setOnClickListener(v -> {
            showToast("Doctors");
            startActivity(new Intent(HistoryActivity.this, DoctorsActivity.class));
        });

        btnAdd.setOnClickListener(v -> {
            showToast("Add New Appointment");
            startActivity(new Intent(HistoryActivity.this, BookingAppointmentActivity.class));
        });

        btnCalendar.setOnClickListener(v -> {
            showToast("Calendar");
            startActivity(new Intent(HistoryActivity.this, CalendarActivity.class));
        });

        btnHistory.setOnClickListener(v -> {
            showToast("Already at History");
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
        if (historyAdapter != null) {
            historyAdapter.stopListening();
        }
    }
}