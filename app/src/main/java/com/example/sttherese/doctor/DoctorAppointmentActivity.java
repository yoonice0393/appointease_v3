package com.example.sttherese.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;


import com.example.sttherese.R;
import com.example.sttherese.patient.activities.CalendarActivity;
import com.example.sttherese.patient.activities.HistoryActivity;
import com.example.sttherese.patient.activities.Home;

public class DoctorAppointmentActivity extends AppCompatActivity {



    private ImageView  btnSearch, btnAdd;
    private LinearLayout layoutDataContent, btnHome, btnAppointment, btnCalendar, btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_doctor_appointment);

        initializeViews();
        setupClickListeners();

    }

    private void initializeViews() {


        btnHome = findViewById(R.id.btnHome);
        btnAppointment = findViewById(R.id.btnAppointment);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnHistory = findViewById(R.id.btnHistory);
        btnAdd = findViewById(R.id.btnAdd);

    }


    private void setupClickListeners() {
        btnHome.setOnClickListener(v -> startActivity(new Intent(this,DoctorHomeActivity.class)));
        btnAppointment.setOnClickListener(v -> Toast.makeText(this, "Already on Appointment", Toast.LENGTH_SHORT).show());
        btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, DoctorCalendarActivity.class)));
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, DoctorHistoryActivity.class)));
        btnAdd.setOnClickListener(v -> Toast.makeText(this, "Add New", Toast.LENGTH_SHORT).show());
    }
}