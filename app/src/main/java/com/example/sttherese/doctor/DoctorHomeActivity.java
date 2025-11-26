package com.example.sttherese.doctor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.example.sttherese.SignInPage;
import com.example.sttherese.adapters.AppointmentAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DoctorHomeActivity extends AppCompatActivity {

    private static final String TAG = "DoctorHomePage";

    private TextView tvGreeting, tvUserName, tvViewAll;
    private ImageView ivNotification, ivProfile,  btnAdd;

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
            Toast.makeText(this, "Authentication required. Please login.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(DoctorHomeActivity.this, SignInPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        initializeViews();
        setupGreeting();
        setupClickListeners();


        // Setup RecyclerView (structure only, query added later)
        rvUpcomingAppointments.setLayoutManager(new LinearLayoutManager(this));
        testFetchAllAppointments();
        // Fetch doctor profile → then appointments
        fetchDoctorProfile();
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
    }

    private void setupGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting = (hour < 12) ? "GOOD MORNING"
                : (hour < 17) ? "GOOD AFTERNOON" : "GOOD EVENING";
        tvGreeting.setText(greeting);
    }

    private void setupClickListeners() {
        ivNotification.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorNotificationActivity.class)));

        ivProfile.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorProfileActivity.class)));

        btnHome.setOnClickListener(v ->
                Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show());

        btnCalendar.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorCalendarActivity.class)));
        btnAppointment.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorAppointmentActivity.class)));

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorHistoryActivity.class)));

        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AvailabilityScheduling.class)));
        tvViewAll.setOnClickListener(v ->
                startActivity(new Intent(this, DoctorAppointmentActivity.class)));
    }
    private void testFetchDoctorAppointments() {
        if (doctorDocId == null) return;

        Log.d(TAG, "TEST: Fetching appointments for doctorId = " + doctorDocId + " (no other filters)");

        db.collection("appointments")
                .whereEqualTo("doctorId", doctorDocId)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots != null) {
                        Log.d(TAG, "TEST: Found " + querySnapshots.size() + " appointments for this doctor");

                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshots.getDocuments()) {
                            Log.d(TAG, "TEST Doctor Appointment - ID: " + doc.getId() +
                                    ", doctorId: '" + doc.getString("doctorId") + "'" +
                                    ", status: '" + doc.getString("status") + "'" +
                                    ", date: '" + doc.getString("date") + "'");
                        }
                    } else {
                        Log.w(TAG, "TEST: No appointments found for doctorId: " + doctorDocId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "TEST: Error: ", e);
                });
    }
    // Add logging to verify the doctorDocId and query
    private void fetchTotalAppointmentCount() {
        if (doctorDocId == null) {
            Log.w(TAG, "Cannot fetch appointment count: doctorDocId is null.");
            return;
        }

        // 🔍 DEBUG: Log the doctorDocId being used
        Log.d(TAG, "Fetching appointments for doctorDocId: " + doctorDocId);

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        // 🔍 DEBUG: Log the date filter
        Log.d(TAG, "Using date filter (today): " + todayDate);

        // Query ALL confirmed appointments from today onwards
        db.collection("appointments")
                .whereEqualTo("doctorId", doctorDocId)
                .whereEqualTo("status", "confirmed")
//                .whereGreaterThanOrEqualTo("date", todayDate)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots != null) {
                        int count = querySnapshots.size();

                        // 🔍 DEBUG: Log the results
                        Log.d(TAG, "Total confirmed appointments found: " + count);

                        // Log each appointment for debugging
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshots.getDocuments()) {
                            Log.d(TAG, "Appointment ID: " + doc.getId() +
                                    ", doctorId: " + doc.getString("doctorId") +
                                    ", status: " + doc.getString("status") +
                                    ", date: " + doc.getString("date"));
                        }

                        tvAppointmentCount.setText(String.format(Locale.getDefault(), "%02d", count));
                    } else {
                        Log.w(TAG, "QuerySnapshots is null");
                        tvAppointmentCount.setText("00");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching total appointment count: ", e);
                    tvAppointmentCount.setText("--");
                });
    }

    // Update your fetchDoctorProfile method to call this new method:
    private void fetchDoctorProfile() {
        if (userDocId == null) return;

        // 🔍 DEBUG: Log the Auth UID being used
        Log.d(TAG, "Fetching doctor profile for Auth UID: " + userDocId);

        db.collection("doctors")
                .whereEqualTo("user_id", userDocId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {

                        com.google.firebase.firestore.DocumentSnapshot snapshot = querySnapshots.getDocuments().get(0);

                        doctorDocId = snapshot.getId();

                        // 🔍 DEBUG: Log the doctor document ID retrieved
                        Log.d(TAG, "Doctor Document ID (doctorDocId): " + doctorDocId);
                        Log.d(TAG, "Doctor user_id field: " + snapshot.getString("user_id"));

                        String rawName = snapshot.getString("name");

                        if (rawName != null) {
                            doctorName = rawName;
                            tvUserName.setText(doctorName);

                            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("doctor_doc_id", doctorDocId);
                            editor.putString("doctor_name", doctorName);
                            editor.apply();
                        }

                        fetchAppointments();
                        fetchTotalAppointmentCount();
                        fetchUniquePatientCount();

                        // 🔍 DEBUG: Call test method AFTER getting doctorDocId
                        testFetchDoctorAppointments();

                    } else {
                        Log.w(TAG, "No doctor profile found for Auth UID: " + userDocId);
                        tvUserName.setText("Doctor!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching doctor profile: ", e);
                    tvUserName.setText("Doctor!");
                });
    }
    private void testFetchAllAppointments() {
        Log.d(TAG, "TEST: Fetching ALL appointments in database");

        db.collection("appointments")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots != null) {
                        Log.d(TAG, "TEST: Total appointments in database: " + querySnapshots.size());

                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshots.getDocuments()) {
                            Log.d(TAG, "TEST Appointment - ID: " + doc.getId() +
                                    ", doctorId: '" + doc.getString("doctorId") + "'" +
                                    ", status: '" + doc.getString("status") + "'" +
                                    ", date: '" + doc.getString("date") + "'");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "TEST: Error fetching all appointments: ", e);
                });
    }
    // Update your fetchAppointments method - remove the count update since it's now separate:
    private void fetchAppointments() {
        if (doctorDocId == null) {
            Log.w(TAG, "Cannot fetch appointments: doctorDocId is null.");
            return;
        }

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        Query appointmentQuery = db.collection("appointments")
                .whereEqualTo("doctorId", doctorDocId)
                .whereEqualTo("status", "pending")
                .whereGreaterThanOrEqualTo("date", todayDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .limit(2);  // Only show 2 in the list

        AppointmentAdapter.OnAppointmentClickListener appointmentClickListener = appointment -> {
            Intent intent = new Intent(DoctorHomeActivity.this, DoctorCalendarActivity.class);
            startActivity(intent);
        };

        appointmentAdapter = new AppointmentAdapter(
                this,
                appointmentClickListener,
                appointmentQuery,
                itemCount -> {
                    // Only control the empty state visibility, not the count
                    if (itemCount > 0) {
                        layoutDataContent.setVisibility(View.VISIBLE);
                        layoutEmptyState.setVisibility(View.GONE);
                    } else {
                        layoutDataContent.setVisibility(View.GONE);
                        layoutEmptyState.setVisibility(View.VISIBLE);
                    }
                },
                "doctor"
        );

        rvUpcomingAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvUpcomingAppointments.setAdapter(appointmentAdapter);
    }

    private void fetchUniquePatientCount() {
        // Ensure we have the doctor's professional ID
        if (doctorDocId == null) {
            Log.w(TAG, "Cannot fetch patient count: doctorDocId is null.");
            return;
        }

        // 1. Query ALL appointments (not just upcoming) for this doctor
        db.collection("appointments")
                .whereEqualTo("doctorId", doctorDocId)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots != null) {
                        // 2. Collect all unique patient UIDs
                        java.util.Set<String> uniquePatientIds = new java.util.HashSet<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshots.getDocuments()) {
                            String patientId = doc.getString("userId"); // The patient's Auth UID
                            if (patientId != null && !patientId.isEmpty()) {
                                uniquePatientIds.add(patientId);
                            }
                        }

                        // 3. Update the Patient Count TextView
                        int count = uniquePatientIds.size();
                        tvPatientCount.setText(String.format(Locale.getDefault(), "%02d", count));

                    } else {
                        tvPatientCount.setText("00");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching unique patient count: ", e);
                    tvPatientCount.setText("--");
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appointmentAdapter != null) {
            appointmentAdapter.removeListener();
        }
    }
}
