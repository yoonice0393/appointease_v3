package com.example.sttherese.doctor;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.MyFirebaseMessagingService;
import com.example.sttherese.R;
import com.example.sttherese.SignInPage;
import com.example.sttherese.adapters.AppointmentAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

public class DoctorHomeActivity extends AppCompatActivity {

    private static final String TAG = "DoctorHomePage";

    private TextView tvGreeting, tvUserName, tvViewAll;
    private ImageView ivNotification, ivProfile,  btnAdd;
    MaterialButton btnGA;

    private RecyclerView rvUpcomingAppointments;
    private LinearLayout layoutDataContent, btnHome, btnAppointment, btnCalendar, btnHistory;
    private androidx.cardview.widget.CardView layoutEmptyState;
    private TextView tvAppointmentCount;
    private TextView tvPatientCount;

    private AppointmentAdapter appointmentAdapter;
    private FirebaseFirestore db;
    private String doctorName;
    private String doctorDocId;
    private String userDocId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_home);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userDocId = prefs.getString("user_doc_id", null);

        if (userDocId == null) {
            startActivity(new Intent(DoctorHomeActivity.this, SignInPage.class));
            finish();
            return;
        }

        initializeViews();
        setupGreeting();
        setupClickListeners();
        requestNotificationPermission();
        
        // Removed setupFCM() from here. It will be called after fetchDoctorProfile()
        
        rvUpcomingAppointments.setLayoutManager(new LinearLayoutManager(this));
        fetchDoctorProfile();
    }

    private void setupFCM() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        Log.d("FCM_TOKEN", "Registering Doctor token: " + token);
                        MyFirebaseMessagingService.sendTokenToServer(this, token);
                    }
                });
    }

    private void fetchDoctorProfile() {
        if (userDocId == null) return;

        db.collection("doctors").whereEqualTo("user_id", userDocId).limit(1).get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot snapshot = querySnapshots.getDocuments().get(0);
                        doctorDocId = snapshot.getId();
                        doctorName = snapshot.getString("name");

                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        prefs.edit().putString("doctor_doc_id", doctorDocId)
                                   .putString("doctor_name", doctorName).apply();

                        tvUserName.setText(doctorName != null ? doctorName : "Doctor!");
                        
                        // CRITICAL: Setup FCM ONLY after doctorDocId is saved
                        setupFCM();
                        
                        fetchAppointments();
                        fetchTotalAppointmentCount();
                        fetchUniquePatientCount();
                    }
                });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void initializeViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvUserName = findViewById(R.id.tvUserName);
        ivNotification = findViewById(R.id.ivNotification);
        ivProfile = findViewById(R.id.ivProfile);
        rvUpcomingAppointments = findViewById(R.id.rvUpcomingAppointments);
        layoutDataContent = findViewById(R.id.layoutDataContent);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvAppointmentCount = findViewById(R.id.tvAppointmentCount);
        tvPatientCount = findViewById(R.id.tvPatientCount);
        tvViewAll=findViewById(R.id.tvViewAllAppointments);
        btnHome = findViewById(R.id.btnHome);
        btnAppointment = findViewById(R.id.btnAppointment);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnHistory = findViewById(R.id.btnHistory);
        btnAdd = findViewById(R.id.btnAdd);
//        btnGA= findViewById(R.id.btnGA);
    }

    private void setupGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        tvGreeting.setText((hour < 12) ? "GOOD MORNING" : (hour < 17) ? "GOOD AFTERNOON" : "GOOD EVENING");
    }

    private void setupClickListeners() {
        ivNotification.setOnClickListener(v -> startActivity(new Intent(this, DoctorNotificationActivity.class)));
        ivProfile.setOnClickListener(v -> startActivity(new Intent(this, DoctorProfileActivity.class)));
        btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, DoctorCalendarActivity.class)));
        btnAppointment.setOnClickListener(v -> startActivity(new Intent(this, DoctorAppointmentActivity.class)));
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, DoctorHistoryActivity.class)));
        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, AvailabilityScheduling.class)));
        tvViewAll.setOnClickListener(v -> startActivity(new Intent(this, DoctorAppointmentActivity.class)));
//        btnGA.setOnClickListener(v -> startActivity(new Intent(this, GASimulationActivity.class)));
    }

    private void fetchTotalAppointmentCount() {
        if (doctorDocId == null) return;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        db.collection("appointments")
                .whereEqualTo("doctorId", doctorDocId)
                .get().addOnSuccessListener(q -> {
                    if (q != null) {
                        int count = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : q.getDocuments()) {
                            String status = doc.getString("status");
                            String date = doc.getString("date");
                            if ("approved".equalsIgnoreCase(status) && date != null && date.compareTo(today) >= 0) {
                                count++;
                            }
                        }
                        tvAppointmentCount.setText(String.format(Locale.getDefault(), "%02d", count));
                    }
                });
    }

    private void fetchAppointments() {
        if (doctorDocId == null) return;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        Query q = db.collection("appointments")
                .whereEqualTo("doctorId", doctorDocId);

        appointmentAdapter = new AppointmentAdapter(this, appointment -> {
            Intent intent = new Intent(this, AppointmentDetailsActivity.class);
            intent.putExtra("appointment_json", new Gson().toJson(appointment));
            startActivity(intent);
        }, q,
                count -> {
                    layoutDataContent.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                    layoutEmptyState.setVisibility(count > 0 ? View.GONE : View.VISIBLE);
                }, "doctor");
        appointmentAdapter.applyFilters("", null, today, Arrays.asList("approved"), 1);
        rvUpcomingAppointments.setAdapter(appointmentAdapter);
    }

    private void fetchUniquePatientCount() {
        if (doctorDocId == null) return;
        db.collection("appointments").whereEqualTo("doctorId", doctorDocId).get().addOnSuccessListener(q -> {
            if (q != null) {
                java.util.Set<String> ids = new java.util.HashSet<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : q.getDocuments()) {
                    String status = doc.getString("status");
                    if (!"approved".equalsIgnoreCase(status)) continue;
                    String pId = doc.getString("userId");
                    if (pId != null) ids.add(pId);
                }
                tvPatientCount.setText(String.format(Locale.getDefault(), "%02d", ids.size()));
            }
        });
    }

    @Override protected void onDestroy() { super.onDestroy(); if (appointmentAdapter != null) appointmentAdapter.removeListener(); }
}
