package com.example.sttherese;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

public class HistoryActivity extends AppCompatActivity {



    // Bottom Navigation
    private LinearLayout btnHome, btnDoctor, btnCalendar, btnHistory;
    private ImageView btnAdd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history);


        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {




        // Bottom Navigation
        btnHome = findViewById(R.id.btnHome);
        btnDoctor = findViewById(R.id.btnDoctor);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnHistory = findViewById(R.id.btnHistory);
        btnAdd = findViewById(R.id.btnAdd);
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
}