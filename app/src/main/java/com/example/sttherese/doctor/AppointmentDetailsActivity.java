package com.example.sttherese.doctor;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sttherese.R;
import com.example.sttherese.models.Appointment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class AppointmentDetailsActivity extends AppCompatActivity {

    private static final String TAG = "ApptDetailsActivity";
    private FirebaseFirestore db;

    // ... (existing view declarations)
    private TextView tvPatientName, tvDate, tvTime, tvSpecialty, tvBirthday, tvAge, tvAddress, tvMedicalHistory;
    private Button statusButton;
    private ImageView closeButton;
    private View progressBar; // Assuming you have a loading indicator

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_details);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupListeners();



        String appointmentJson = getIntent().getStringExtra("appointment_json");

        if (appointmentJson != null) {
            Appointment appointment = new Gson().fromJson(appointmentJson, Appointment.class);
            if (appointment != null) {
                // Populate initial appointment details
                populateDetails(appointment);

                // Fetch patient specific details from the 'patients' collection
                fetchPatientDetails(appointment.getUserId());
            } else {
                Toast.makeText(this, "Failed to load appointment details.", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Appointment data missing.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {

        tvPatientName = findViewById(R.id.tvPatientName);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvSpecialty = findViewById(R.id.tvSpecialty);
        statusButton = findViewById(R.id.statusButton);
        tvBirthday = findViewById(R.id.tvBirthday);
        tvAge = findViewById(R.id.tvAge);
        tvAddress = findViewById(R.id.tvAddress);
        tvMedicalHistory = findViewById(R.id.tvMedicalHistory);
        closeButton = findViewById(R.id.closeButton);
        // progressBar = findViewById(R.id.progressBar); // If you have one
    }

    private void setupListeners() {
        closeButton.setOnClickListener(v -> finish());
    }

    private void populateDetails(Appointment appointment) {
        // Set details already available in the Appointment object
        tvPatientName.setText(appointment.getPatientName());

        // --- CHANGE 1: Format Appointment Date ---
        String formattedDate = formatFirestoreDate(appointment.getDate());
        tvDate.setText(formattedDate);

        tvTime.setText(appointment.getTime());
        tvSpecialty.setText(appointment.getSpecialty());
        statusButton.setText(appointment.getStatus());

        // Set placeholders for patient data until fetched
        tvBirthday.setText("Loading...");
        tvAge.setText("Loading...");
        tvAddress.setText("Loading...");
        tvMedicalHistory.setText("Medical history pending implementation.");
    }

    private void fetchPatientDetails(String authUserId) {
        if (authUserId == null || authUserId.isEmpty()) {
            Log.e(TAG, "Auth User ID is null or empty. Cannot fetch patient details.");
            tvBirthday.setText("N/A");
            tvAge.setText("N/A");
            tvAddress.setText("N/A");
            return;
        }

        // --- CORRECTED LOGIC: Query by the 'userId' field (Auth ID) ---
        db.collection("patients")
                .whereEqualTo("userId", authUserId) // This matches the Auth ID to the field in the patient document
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (!querySnapshot.isEmpty()) {
                        // Get the first (and should be only) matching patient document
                        Map<String, Object> patientData = querySnapshot.getDocuments().get(0).getData();

                        // Extract data based on your Firebase screenshot
                        String dob = (String) patientData.get("dob");
                        // Firestore reads integer numbers as Long
                        Object ageObj = patientData.get("age");
                        String address = (String) patientData.get("address");

                        if (dob != null) {
                            String formattedDob = formatFirestoreDate(dob);
                            tvBirthday.setText(formattedDob);
                        } else {
                            tvBirthday.setText("N/A");
                        }
                        if (ageObj != null) tvAge.setText(String.valueOf(ageObj)); else tvAge.setText("N/A");
                        if (address != null) tvAddress.setText(address); else tvAddress.setText("N/A");

                    } else {
                        Log.w(TAG, "Patient document not found for userId: " + authUserId);
                        tvBirthday.setText("N/A");
                        tvAge.setText("N/A");
                        tvAddress.setText("N/A");
                        Toast.makeText(this, "Patient details not found (Query successful, but empty).", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching patient details: " + e.getMessage());
                    tvBirthday.setText("Error");
                    tvAge.setText("Error");
                    tvAddress.setText("Error");
                    Toast.makeText(this, "Error fetching patient details.", Toast.LENGTH_SHORT).show();
                });
    }


    private String formatFirestoreDate(String dateString) {
        // The format Firestore uses (YYYY-MM-DD)
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        // The desired output format (Month DD, YYYY)
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

        try {
            Date date = inputFormat.parse(dateString);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Date parsing error for: " + dateString, e);
        }
        return dateString; // Return original string if formatting fails
    }
}