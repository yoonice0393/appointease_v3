package com.example.sttherese.doctor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.adapters.AppointmentAdapter;
import com.example.sttherese.models.Appointment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DoctorAppointmentActivity extends AppCompatActivity
        implements AppointmentAdapter.OnAppointmentClickListener,
        AppointmentAdapter.OnDataStatusChangeListener {

    private FirebaseFirestore db;
    private ImageView btnSearch, btnAdd;
    private ChipGroup chipGroup;
    private EditText etSearch;

    private LinearLayout btnHome, btnAppointment, btnCalendar, btnHistory;

    private RecyclerView recyclerView;
    private AppointmentAdapter appointmentAdapter;
    private TextView tvNoData;

    private String doctorDocId;
    private String currentChipFilter = "All";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_appointment);

        db = FirebaseFirestore.getInstance();

        // Get the Doctor's Doc ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        doctorDocId = prefs.getString("doctor_doc_id", null);

        if (doctorDocId == null) {
            Toast.makeText(this, "Doctor ID not found. Please re-login.", Toast.LENGTH_LONG).show();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initializeViews() {
        btnHome = findViewById(R.id.btnHome);
        btnAppointment = findViewById(R.id.btnAppointment);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnHistory = findViewById(R.id.btnHistory);
        btnAdd = findViewById(R.id.btnAdd);

        recyclerView = findViewById(R.id.recyclerViewAppointments);
        tvNoData = findViewById(R.id.tvNoData);

        chipGroup = findViewById(R.id.chipGroup);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
    }

    private void setupClickListeners() {
        btnHome.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorHomeActivity.class)));

        btnAppointment.setOnClickListener(v ->
                Toast.makeText(this, "Already on Appointment", Toast.LENGTH_SHORT).show());

        btnCalendar.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorCalendarActivity.class)));

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorHistoryActivity.class)));

        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AvailabilityScheduling.class)));

        // Chip Group Listener - Fixed to handle deselection properly
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentChipFilter = "All";
            } else {
                Chip selectedChip = findViewById(checkedIds.get(0));
                currentChipFilter = selectedChip.getText().toString();
            }

            // Reload appointments with new chip filter
            loadAppointments(currentChipFilter, currentSearchQuery);
        });

        // Real-time search as user types
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();

                // Apply filter in real-time
                if (appointmentAdapter != null) {
                    appointmentAdapter.filterByName(currentSearchQuery);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Search button - triggers search
        btnSearch.setOnClickListener(v -> {
            currentSearchQuery = etSearch.getText().toString().trim();

            if (appointmentAdapter != null) {
                appointmentAdapter.filterByName(currentSearchQuery);
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadAppointments("All", "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appointmentAdapter != null) {
            appointmentAdapter.removeListener();
        }
    }

    private void loadAppointments(String chipFilter, String searchName) {
        if (doctorDocId == null) {
            Toast.makeText(this, "Doctor ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        // Base Query Setup
        Query baseQuery = db.collection("appointments")
                .whereEqualTo("doctorId", doctorDocId)
                .orderBy("date", Query.Direction.ASCENDING);

        // Apply Date/Chip Filter
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        if ("Today".equalsIgnoreCase(chipFilter)) {
            String todayDate = dateFormat.format(calendar.getTime());
            baseQuery = baseQuery.whereEqualTo("date", todayDate);
        } else if ("Tomorrow".equalsIgnoreCase(chipFilter)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            String tomorrowDate = dateFormat.format(calendar.getTime());
            baseQuery = baseQuery.whereEqualTo("date", tomorrowDate);
        }
        // "All" or "Upcoming" - no additional date filter needed

        // Remove old listener
        if (appointmentAdapter != null) {
            appointmentAdapter.removeListener();
        }

        // Initialize new adapter
        appointmentAdapter = new AppointmentAdapter(
                this,
                this,
                baseQuery,
                new AppointmentAdapter.OnDataStatusChangeListener() {
                    @Override
                    public void onDataLoaded(int itemCount) {
                        // Apply search filter after data is loaded
                        if (searchName != null && !searchName.trim().isEmpty()) {
                            appointmentAdapter.filterByName(searchName);
                        } else {
                            DoctorAppointmentActivity.this.onDataLoaded(itemCount);
                        }
                    }
                },
                "doctor"
        );

        recyclerView.setAdapter(appointmentAdapter);
    }

    @Override
    public void onItemClick(Appointment appointment) {
        // 1. Create an Intent for the details activity
        Intent intent = new Intent(this, AppointmentDetailsActivity.class);

        // 2. Convert the Appointment object to a JSON string
        // This is a reliable way to pass complex objects between activities.
        String appointmentJson = new Gson().toJson(appointment);

        // 3. Put the JSON string into the Intent
        intent.putExtra("appointment_json", appointmentJson);

        // 4. Start the new activity
        startActivity(intent);
    }

    @Override
    public void onDataLoaded(int itemCount) {
        if (itemCount == 0) {
            findViewById(R.id.layoutEmptyState).setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            findViewById(R.id.layoutEmptyState).setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}