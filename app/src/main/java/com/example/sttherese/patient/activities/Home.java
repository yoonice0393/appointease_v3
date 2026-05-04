        package com.example.sttherese.patient.activities;

        import android.Manifest;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.content.pm.PackageManager;
        import android.os.Build;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.EditText;
        import android.widget.ImageView;
        import android.widget.LinearLayout;
        import android.widget.ProgressBar;
        import android.widget.TextView;
        import android.widget.Toast;

        import androidx.appcompat.app.AppCompatActivity;
        import androidx.cardview.widget.CardView;
        import androidx.core.app.ActivityCompat;
        import androidx.core.content.ContextCompat;
        import androidx.recyclerview.widget.LinearLayoutManager;
        import androidx.recyclerview.widget.RecyclerView;

        import com.example.sttherese.MyFirebaseMessagingService;
        import com.example.sttherese.R;
        import com.example.sttherese.SignInPage;
        import com.example.sttherese.adapters.AppointmentAdapter;
        import com.example.sttherese.adapters.DoctorAdapter;
        import com.example.sttherese.adapters.ScheduleSlotAdapter;
        import com.example.sttherese.models.ScheduleSlot;
        import com.google.android.material.button.MaterialButton;
        import com.google.android.material.chip.Chip;
        import com.google.android.material.chip.ChipGroup;
        import com.google.firebase.firestore.DocumentSnapshot;
        import com.google.firebase.firestore.FirebaseFirestore;
        import com.google.firebase.firestore.Query;
        import com.google.firebase.messaging.FirebaseMessaging;

        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Calendar;
        import java.util.Date;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Locale;
        import java.util.Map;

        public class Home extends AppCompatActivity {

            private static final String TAG = "HomePage";

            private TextView tvGreeting, tvUserName;
            private ImageView ivNotification, ivProfile;
            private RecyclerView rvUpcomingAppointments, rvDoctors;
            private AppointmentAdapter appointmentAdapter;
            private DoctorAdapter doctorAdapter;
            private LinearLayout layoutAppointments;
            private CardView layoutEmptyState;
            private MaterialButton btnBookAppointment;
            private ProgressBar progressBar;
            private ChipGroup chipGroup;
            private Chip chipAll, chipObGyne, chipMedical;
            private LinearLayout btnHome, btnDoctor, btnCalendar, btnHistory;
            private ImageView btnAdd;
            private FirebaseFirestore db;
            private String userDocId;
            private TextView tvVisitDate, tvVisitDay, tvServiceType, tvViewAll;
            private CardView recentVisitCard, emptyRecentVisitCard;

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_home2);

                db = FirebaseFirestore.getInstance();
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                userDocId = prefs.getString("user_doc_id", null);

                if (userDocId == null) {
                    startActivity(new Intent(this, SignInPage.class));
                    finish();
                    return;
                }

                initializeViews();
                setupRecyclerViews();
                setupGreeting();
                setupClickListeners();
                requestNotificationPermission();
                fetchUserName(); // setupFCM is now called inside this
                fetchAppointments();
                fetchDoctors("All");
                fetchRecentVisit();

                btnBookAppointment.setOnClickListener(v -> startActivity(new Intent(Home.this, BookingAppointmentActivity.class)));
                btnAdd.setOnClickListener(v -> startActivity(new Intent(Home.this, BookingAppointmentActivity.class)));
            }
            
            private void setupFCM() {
                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                String token = task.getResult();
                                Log.d("FCM_TOKEN", "Registering token for Patient: " + token);
                                MyFirebaseMessagingService.sendTokenToServer(this, token);
                            }
                        });
            }
            
            private void requestNotificationPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
                rvDoctors = findViewById(R.id.rvDoctors);
                layoutAppointments = findViewById(R.id.layoutAppointments);
                layoutEmptyState = findViewById(R.id.layoutEmptyState);
                btnBookAppointment = findViewById(R.id.btnBookAppointment);
                progressBar = findViewById(R.id.progressBar);
                chipGroup = findViewById(R.id.chipGroup);
                chipAll = findViewById(R.id.chipAll);
                chipObGyne = findViewById(R.id.chipObGyne);
                chipMedical = findViewById(R.id.chipMedical);
                tvViewAll = findViewById(R.id.tvViewAll);
                btnHome = findViewById(R.id.btnHome);
                btnDoctor = findViewById(R.id.btnDoctor);
                btnCalendar = findViewById(R.id.btnCalendar);
                btnHistory = findViewById(R.id.btnHistory);
                btnAdd = findViewById(R.id.btnAdd);
                tvVisitDate = findViewById(R.id.tvVisitDate);
                tvVisitDay = findViewById(R.id.tvVisitDay);
                tvServiceType = findViewById(R.id.tvServiceType);
                recentVisitCard = findViewById(R.id.recentVisitCard); 
                emptyRecentVisitCard = findViewById(R.id.emptyRecentVisitCard);
                if (chipAll != null) chipAll.setChecked(true);
            }

            private void fetchUserName() {
                db.collection("patients").whereEqualTo("userId", userDocId).limit(1)
                        .addSnapshotListener((querySnapshots, e) -> {
                            if (querySnapshots != null && !querySnapshots.isEmpty()) {
                                DocumentSnapshot snapshot = querySnapshots.getDocuments().get(0);
                                
                                // CRITICAL: Save ID and THEN register FCM
                                String firestoreId = snapshot.getId();
                                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                prefs.edit().putString("patient_firestore_id", firestoreId).apply();
                                
                                setupFCM(); // Token registration now uses correct Firestore ID
                                
                                String firstName = snapshot.getString("first_name");
                                tvUserName.setText((firstName != null ? firstName : "User") + "!");
                            }
                        });
            }

            private void fetchRecentVisit() {
                db.collection("appointments").whereEqualTo("userId", userDocId).whereEqualTo("status", "completed")
                        .orderBy("date", Query.Direction.DESCENDING).limit(1).get()
                        .addOnSuccessListener(querySnapshots -> {
                            if (querySnapshots != null && !querySnapshots.isEmpty()) {
                                DocumentSnapshot doc = querySnapshots.getDocuments().get(0);
                                tvServiceType.setText(doc.getString("specialty"));
                                recentVisitCard.setVisibility(View.VISIBLE);
                                emptyRecentVisitCard.setVisibility(View.GONE);
                            } else {
                                recentVisitCard.setVisibility(View.GONE);
                                emptyRecentVisitCard.setVisibility(View.VISIBLE);
                            }
                        });
            }

            private void setupRecyclerViews() {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                Query q = db.collection("appointments").whereEqualTo("userId", userDocId).whereEqualTo("status", "pending")
                        .whereGreaterThanOrEqualTo("date", today).orderBy("date", Query.Direction.ASCENDING).limit(1);

                appointmentAdapter = new AppointmentAdapter(this, a -> startActivity(new Intent(Home.this, CalendarActivity.class)), q, 
                        count -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            layoutAppointments.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                            layoutEmptyState.setVisibility(count > 0 ? View.GONE : View.VISIBLE);
                        }, "patient");

                rvUpcomingAppointments.setLayoutManager(new LinearLayoutManager(this));
                rvUpcomingAppointments.setAdapter(appointmentAdapter);

                doctorAdapter = new DoctorAdapter(this, doctor -> {}, db.collection("doctors"));
                rvDoctors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                rvDoctors.setAdapter(doctorAdapter);
            }

            private void setupGreeting() {
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                tvGreeting.setText((hour < 12) ? "GOOD MORNING" : (hour < 17) ? "GOOD AFTERNOON" : "GOOD EVENING");
            }

            private void setupClickListeners() {
                ivNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
                ivProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
                tvViewAll.setOnClickListener(v -> startActivity(new Intent(this, DoctorsActivity.class)));
                btnDoctor.setOnClickListener(v -> startActivity(new Intent(this, DoctorsActivity.class)));
                btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
                btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
            }

            private void fetchAppointments() { if (progressBar != null) progressBar.setVisibility(View.VISIBLE); }
            private void fetchDoctors(String specialty) { /* logic to refresh doctor list */ }

            @Override protected void onResume() { super.onResume(); setupGreeting(); }
            @Override protected void onDestroy() { super.onDestroy(); if (appointmentAdapter != null) appointmentAdapter.removeListener(); }
        }