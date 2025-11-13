package com.example.sttherese.patient.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.adapters.DoctorAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class DoctorsActivity extends AppCompatActivity {

    private RecyclerView rvDoctors;
    private DoctorAdapter doctorAdapter;

    private EditText etSearch;
    private ImageView btnSearch;
    private ChipGroup chipGroup;
    private Chip chipAll, chipObGyn, chipPerinatologist;

    // Bottom Navigation
    private LinearLayout btnHome, btnDoctor, btnCalendar, btnHistory;
    private ImageView btnAdd;

    private FirebaseFirestore db;

    private String selectedFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctors);

        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupFilters();
        setupSearch();
        setupRecyclerView();
        setupClickListeners();
        // Load initial doctors
        fetchDoctors(selectedFilter);
    }

    private void setupClickListeners() {
        btnHome.setOnClickListener(v -> startActivity(new Intent(this, Home.class)));
        btnDoctor.setOnClickListener(v -> Toast.makeText(this, "Already on Doctors", Toast.LENGTH_SHORT).show());
        btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        btnAdd.setOnClickListener(v -> Toast.makeText(this, "Add New", Toast.LENGTH_SHORT).show());
    }

    private void initializeViews() {
        rvDoctors = findViewById(R.id.rvDoctors);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);

        chipGroup = findViewById(R.id.chipGroup);
        chipAll = findViewById(R.id.chipAll);
        chipObGyn = findViewById(R.id.chipObGyn);
        chipPerinatologist = findViewById(R.id.chipPerinatologist);


        btnHome = findViewById(R.id.btnHome);
        btnDoctor = findViewById(R.id.btnDoctor);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnHistory = findViewById(R.id.btnHistory);
        btnAdd = findViewById(R.id.btnAdd);

        rvDoctors.setLayoutManager(new LinearLayoutManager(this));
        chipAll.setChecked(true);
    }

    private void setupRecyclerView() {
        // Adapter will be initialized when fetching from Firestore
    }

    private void setupFilters() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int selectedId = checkedIds.get(0);
                if (selectedId == R.id.chipAll) selectedFilter = "All";
                else if (selectedId == R.id.chipObGyn) selectedFilter = "OB-GYN";
                else if (selectedId == R.id.chipPerinatologist) selectedFilter = "Perinatologist";

                fetchDoctors(selectedFilter);
            }
        });
    }

    private void setupSearch() {
        btnSearch.setOnClickListener(v -> {
            String queryText = etSearch.getText().toString().trim();
            performSearch(queryText);
        });
    }

    private void fetchDoctors(String specialty) {
        Query query = db.collection("doctors");
        if (!specialty.equals("All")) {
            query = query.whereEqualTo("specialty", specialty);
        }

        // Remove previous listener if exists
        if (doctorAdapter != null) doctorAdapter.removeListener();

        doctorAdapter = new DoctorAdapter(this, doctor -> {
            Toast.makeText(DoctorsActivity.this,
                    "Booking appointment with " + doctor.getName(),
                    Toast.LENGTH_SHORT).show();
        }, query);

        rvDoctors.setAdapter(doctorAdapter);
    }

    private void performSearch(String searchQuery) {
        Query query = db.collection("doctors")
                .orderBy("name")
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff");

        if (doctorAdapter != null) doctorAdapter.removeListener();

        doctorAdapter = new DoctorAdapter(this, doctor -> {
            Toast.makeText(DoctorsActivity.this,
                    "Booking appointment with " + doctor.getName(),
                    Toast.LENGTH_SHORT).show();
        }, query);

        rvDoctors.setAdapter(doctorAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (doctorAdapter != null) doctorAdapter.removeListener();
    }
}
