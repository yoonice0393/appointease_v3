package com.example.sttherese.patient.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.adapters.HistoryAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.sttherese.adapters.OnItemCountChangeListener;

public class HistoryActivity extends AppCompatActivity implements OnItemCountChangeListener {

    private static final String TAG = "HistoryActivity";

    // Firestore
    private FirebaseFirestore db;
    private String userDocId;

    // UI Views
    private RecyclerView rvHistoryAppointments;
    private CardView cardEmptyHistory;

    // Adapter
    private HistoryAdapter historyAdapter;

    // Bottom Navigation
    private LinearLayout btnHome, btnDoctor, btnCalendar, btnHistory;
    private ImageView btnAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userDocId = prefs.getString("user_doc_id", null);

        initializeViews();
        setupClickListeners();

        if (userDocId != null) {
            setupHistoryList();
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            // Show empty state if user is not logged in
            cardEmptyHistory.setVisibility(View.VISIBLE);
            rvHistoryAppointments.setVisibility(View.GONE);
        }
    }

    private void initializeViews() {
        // History specific views
        rvHistoryAppointments = findViewById(R.id.rvHistoryAppointments);
        cardEmptyHistory = findViewById(R.id.cardEmptyHistory);

        // Bottom Navigation
        btnHome = findViewById(R.id.btnHome);
        btnDoctor = findViewById(R.id.btnDoctor);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnHistory = findViewById(R.id.btnHistory);
        btnAdd = findViewById(R.id.btnAdd);
    }

    private void setupHistoryList() {
        // Query: Filter by current user, filter by status "completed", sort by date descending
        Query historyQuery = db.collection("appointments").whereEqualTo("userId", userDocId).whereEqualTo("status", "completed").orderBy("date", Query.Direction.DESCENDING);

        // Initialize Adapter
        historyAdapter = new HistoryAdapter(historyQuery, this); // Pass 'this' as the listener

        rvHistoryAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvHistoryAppointments.setAdapter(historyAdapter);
    }

    // --- OnItemCountChangeListener implementation ---
    @Override
    public void onCountChange(int count) {
        if (count > 0) {
            // Show the list
            rvHistoryAppointments.setVisibility(View.VISIBLE);
            cardEmptyHistory.setVisibility(View.GONE);
        } else {
            // Show the empty state
            rvHistoryAppointments.setVisibility(View.GONE);
            cardEmptyHistory.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {


// Bottom Navigation

        btnHome.setOnClickListener(v -> {

            showToast("Home");

            startActivity(new Intent(HistoryActivity.this, Home.class));

// Already on home page

        });


        btnDoctor.setOnClickListener(v -> {

            showToast("Doctors");
// Navigate to DoctorsActivity
            startActivity(new Intent(HistoryActivity.this, DoctorsActivity.class));
        });

        btnAdd.setOnClickListener(v -> {
            showToast("Add New Appointment");
// Navigate to AddAppointmentActivity
// startActivity(new Intent(HomePage.this, AddAppointmentActivity.class));

        });


        btnCalendar.setOnClickListener(v -> {

            showToast("Calendar");

// Navigate to CalendarActivity

            startActivity(new Intent(HistoryActivity.this, CalendarActivity.class));

        });


        btnHistory.setOnClickListener(v -> {

            showToast("Already at History");

// Show menu dialog or navigate to MenuActivity

// showMenuDialog();

        });

    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (historyAdapter != null) {
            historyAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (historyAdapter != null) {
            historyAdapter.stopListening();
        }
    }
}