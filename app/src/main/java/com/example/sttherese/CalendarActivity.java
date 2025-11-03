package com.example.sttherese;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.prolificinteractive.materialcalendarview.MaterialCalendarView;


public class CalendarActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView doctorName, appointmentDate, appointmentTime;
    // Bottom Navigation
    private LinearLayout btnHome, btnDoctor, btnCalendar, btnHistory;
    private ImageView btnAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        calendarView = findViewById(R.id.calendarView);
        doctorName = findViewById(R.id.tvDoctorName);
        appointmentDate = findViewById(R.id.tvAppointmentDate);
        appointmentTime = findViewById(R.id.tvAppointmentTime);

        // Set listener for date selection
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            String selectedDate = date.getDay() + " " + date.getMonth() + " " + date.getYear();
            appointmentDate.setText(selectedDate);

            // You can dynamically fetch appointment details from your DB here
        });

        initializeViews();
        setupClickListeners();
    }


    private void initializeViews() {


        // Upcoming Appointment
//        appointmentCard = findViewById(R.id.appointmentCard);
//        tvDoctorName = findViewById(R.id.tvDoctorName);
//        tvDoctorSpecialty = findViewById(R.id.tvDoctorSpecialty);
//        tvAppointmentDate = findViewById(R.id.tvAppointmentDate);
//        tvAppointmentTime = findViewById(R.id.tvAppointmentTime);
//        btnNavigate = findViewById(R.id.btnNavigate);


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
            startActivity(new Intent(CalendarActivity.this, Home.class));
            // Already on home page
        });

        btnDoctor.setOnClickListener(v -> {
            showToast("Doctors");
            // Navigate to DoctorsActivity
            startActivity(new Intent(CalendarActivity.this, DoctorsActivity.class));
        });

        btnAdd.setOnClickListener(v -> {
            showToast("Add New Appointment");
            // Navigate to AddAppointmentActivity
            // startActivity(new Intent(HomePage.this, AddAppointmentActivity.class));
        });

        btnCalendar.setOnClickListener(v -> {
            showToast("Already at Calendar");
            // Navigate to CalendarActivity
//            startActivity(new Intent(CalendarActivity.this, CalendarActivity.class));
        });

        btnHistory.setOnClickListener(v -> {
            showToast("History");
            // Show menu dialog or navigate to MenuActivity
            startActivity(new Intent(CalendarActivity.this, HistoryActivity.class));
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}