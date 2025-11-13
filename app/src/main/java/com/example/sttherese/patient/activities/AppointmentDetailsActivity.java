package com.example.sttherese.patient.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;

import com.example.sttherese.R;
import com.example.sttherese.adapters.DoctorAdapter;
import com.example.sttherese.models.Doctor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;

import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppointmentDetailsActivity extends AppCompatActivity {

    private MaterialAutoCompleteTextView spinnerAppointmentType;
    private CardView doctorCard;
    private ImageView doctorImageView, closeButton;
    private TextView doctorNameText, doctorSpecialtyText;
    private Button buttonPickDate;
    private TextView textSelectedDate;
    private GridLayout gridMorning, gridAfternoon;
    private MaterialButton buttonBook;

    private FirebaseFirestore db;
    private String selectedDoctorId = null;
    private String selectedDate = null;
    private String selectedTime = null;
    private String selectedAppointmentType = null;

    private List<String> appointmentTypes = new ArrayList<>();
    private Button selectedTimeButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_details);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Setup listeners
        setupListeners();

        // Fetch data from Firebase
        fetchAppointmentTypes();
    }

    private void initializeViews() {
        spinnerAppointmentType = findViewById(R.id.spinnerAppointmentType);
        doctorCard = findViewById(R.id.doctorCard);
        buttonPickDate = findViewById(R.id.buttonPickDate);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        gridMorning = findViewById(R.id.gridMorning);
        gridAfternoon = findViewById(R.id.gridAfternoon);
        buttonBook = findViewById(R.id.buttonBook);
        closeButton = findViewById(R.id.closeButton);
    }

    private void setupListeners() {
        // Close button
        closeButton.setOnClickListener(v -> finish());

        // Appointment type selection
        spinnerAppointmentType.setOnItemClickListener((parent, view, position, id) -> {
            selectedAppointmentType = appointmentTypes.get(position);
            fetchDoctorsByType(selectedAppointmentType);
        });

        // Doctor card click
        doctorCard.setOnClickListener(v -> {
            if (selectedAppointmentType != null) {
                showDoctorSelectionDialog();
            } else {
                Toast.makeText(this, "Please select appointment type first", Toast.LENGTH_SHORT).show();
            }
        });

        // Date picker
        buttonPickDate.setOnClickListener(v -> showDatePicker());

        // Book appointment button
        buttonBook.setOnClickListener(v -> bookAppointment());
    }

    private void fetchAppointmentTypes() {
        db.collection("appointmentTypes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    appointmentTypes.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String type = document.getString("name");
                        if (type != null) {
                            appointmentTypes.add(type);
                        }
                    }

                    // Setup dropdown adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            R.layout.dropdown_item,
                            appointmentTypes
                    );
                    spinnerAppointmentType.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load appointment types", Toast.LENGTH_SHORT).show();
                });
    }

    // New class variable to hold the query for the adapter
    private Query doctorsQuery = null;
    // New class variable to hold the selected Doctor object
    private Doctor selectedDoctor = null;

    private void fetchDoctorsByType(String appointmentType) {
        // 1. Create the Query object
        doctorsQuery = db.collection("doctors")
                .whereEqualTo("specialty", appointmentType); // Use "specialty" (fixed from earlier)

        // 2. Fetch the initial result immediately to auto-select the first doctor
        //    and update the main card, just like the old logic.
        doctorsQuery.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot firstDoc = querySnapshot.getDocuments().get(0);

                        // Convert to Doctor model
                        selectedDoctor = firstDoc.toObject(Doctor.class);

                        // FIX: The red line is here: You must explicitly set the ID
                        // because toObject() doesn't populate the transient 'id' field.
                        selectedDoctor.setId(firstDoc.getId());

                        // Assigning selectedDoctorId using the ID from the model
                        selectedDoctorId = selectedDoctor.getId();

                        updateDoctorCard(firstDoc);
                    } else {
                        // Reset selected doctor if none are found
                        selectedDoctorId = null;
                        selectedDoctor = null;
                        Toast.makeText(this, "No doctors available for this type", Toast.LENGTH_SHORT).show();
                        // Optional: Reset doctor card to default state here
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load doctors", Toast.LENGTH_SHORT).show();
                });
    }

    private DoctorAdapter doctorAdapter; // Class variable for the adapter

    private void showDoctorSelectionDialog() {
        if (doctorsQuery == null) {
            Toast.makeText(this, "Please select appointment type first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use AlertDialog Builder to create a custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_doctor_selection, null);
        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();

        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewDoctors);
        Button buttonCloseDialog = dialogView.findViewById(R.id.buttonCloseDialog);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create the listener implementation
        DoctorAdapter.OnDoctorClickListener listener = doctor -> {
            // 1. Update selected doctor in the Activity
            // This line is correct, as 'doctor' is an instance of com.example.sttherese.models.Doctor
            selectedDoctor = doctor;
            selectedDoctorId = doctor.getId();

            // 2. Fetch the document snapshot to pass to updateDoctorCard(DocumentSnapshot)
            //    We need to fetch the document because updateDoctorCard expects a DocumentSnapshot,
            //    but the listener gives us a Doctor model.
            db.collection("doctors").document(selectedDoctorId).get()
                    .addOnSuccessListener(doc -> updateDoctorCard(doc));

            // 3. Clear date/time selection since the doctor has changed
            selectedDate = null;
            selectedTime = null;
            textSelectedDate.setText("No date selected");
            gridMorning.removeAllViews();
            gridAfternoon.removeAllViews();

            Toast.makeText(this, doctor.getName() + " selected.", Toast.LENGTH_SHORT).show();

            // 4. Dismiss the dialog
            alertDialog.dismiss();
        };

        // Initialize the adapter using your existing adapter class and the query
        doctorAdapter = new DoctorAdapter(this, listener, doctorsQuery);
        recyclerView.setAdapter(doctorAdapter);

        buttonCloseDialog.setOnClickListener(v -> alertDialog.dismiss());

        // Clean up the listener when the dialog is dismissed
        alertDialog.setOnDismissListener(dialog -> {
            if (doctorAdapter != null) {
                doctorAdapter.removeListener();
            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        alertDialog.show();
    }
    private boolean isDayAvailable(Calendar selectedCalendar, String scheduleDays) {
        if (scheduleDays == null || scheduleDays.isEmpty()) {
            // If schedule is not set, assume doctor is not available for booking
            return false;
        }

        // Get day of week: Calendar.SUNDAY=1, Calendar.MONDAY=2, ...
        int dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK);

        // Convert the Calendar day to a short string (e.g., "Mon", "Sun")
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.US); // Use Locale.US for consistent English day names
        String selectedDayShort = dayFormat.format(selectedCalendar.getTime()); // e.g., "Mon"

        // Special case for "Monday to Saturday" as seen in your Firestore image
        if (scheduleDays.toLowerCase(Locale.ROOT).contains("to")) {
            // Only allow days Monday (2) through Saturday (7)
            return dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.SATURDAY;
        }

        // For comma-separated lists (e.g., "Mon, Wed, Fri")
        // Note: We check if the doctor's schedule string contains the short day name
        if (scheduleDays.toLowerCase(Locale.ROOT).contains(selectedDayShort.toLowerCase(Locale.ROOT))) {
            return true;
        }

        return false;
    }
    private void updateDoctorCard(DocumentSnapshot doctor) {
        // Inflate doctor card content
        View doctorView = LayoutInflater.from(this).inflate(R.layout.doctor_card_content, null);
        doctorImageView = doctorView.findViewById(R.id.doctorImage);
        doctorNameText = doctorView.findViewById(R.id.doctorName);
        doctorSpecialtyText = doctorView.findViewById(R.id.doctorSpecialty);

        // Set doctor info
        doctorNameText.setText(doctor.getString("name"));
        doctorSpecialtyText.setText(doctor.getString("specialty"));

        String imageUrl = doctor.getString("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_doctor_placeholder)
                    .into(doctorImageView);
        }

        // Clear and update card view
        doctorCard.removeAllViews();
        doctorCard.addView(doctorView);
    }

    private void showDatePicker() {
        if (selectedDoctor == null) {
            Toast.makeText(this, "Please select a doctor first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Get the doctor's schedule days string
        String doctorSchedule = selectedDoctor.getScheduleDays();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    // --- Schedule Check Logic ---
                    if (!isDayAvailable(selectedCalendar, doctorSchedule)) {
                        Toast.makeText(this, "Doctor is not available on that day.", Toast.LENGTH_LONG).show();
                        // Clear any previous date selection if invalid
                        textSelectedDate.setText("No date selected");
                        selectedDate = null;
                        return;
                    }
                    // --- End Schedule Check Logic ---

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = sdf.format(selectedCalendar.getTime());

                    SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                    textSelectedDate.setText(displayFormat.format(selectedCalendar.getTime()));

                    // Fetch available time slots
                    if (selectedDoctorId != null) {
                        fetchTimeSlots(selectedDoctorId, selectedDate);
                    }
                },
                year, month, day
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void fetchTimeSlots(String doctorId, String date) {
        // Clear existing slots
        gridMorning.removeAllViews();
        gridAfternoon.removeAllViews();

        db.collection("appointments")
                .document(doctorId)
                .collection(date)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // No slots configured, show default slots
                        createDefaultTimeSlots();
                    } else {
                        // Display slots from Firebase
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            String time = document.getString("time");
                            Boolean available = document.getBoolean("available");

                            if (time != null && available != null) {
                                addTimeSlotButton(time, available);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load time slots", Toast.LENGTH_SHORT).show();
                    createDefaultTimeSlots();
                });
    }

    private void createDefaultTimeSlots() {
        // Morning slots
        String[] morningSlots = {"9:00 am", "9:30 am", "10:00 am", "10:30 am", "11:00 am", "11:30 am"};
        for (String time : morningSlots) {
            addTimeSlotButton(time, true);
        }

        // Afternoon slots
        String[] afternoonSlots = {"1:00 pm", "1:30 pm", "2:00 pm", "2:30 pm", "3:00 pm", "3:30 pm", "4:00 pm", "4:30 pm", "5:00 pm"};
        for (String time : afternoonSlots) {
            addTimeSlotButton(time, true);
        }
    }

    private void addTimeSlotButton(String time, boolean available) {
        Button timeButton = new Button(this);
        timeButton.setText(time);
        timeButton.setEnabled(available);

        // Apply custom style
        if (available) {
            timeButton.setBackgroundResource(R.drawable.time_slot_selector);
        } else {
            timeButton.setBackgroundResource(R.drawable.time_slot_unavailable);
        }

        // Set layout params
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(8, 8, 8, 8);
        timeButton.setLayoutParams(params);

        // Click listener
        timeButton.setOnClickListener(v -> {
            if (selectedTimeButton != null) {
                selectedTimeButton.setSelected(false);
            }
            selectedTimeButton = timeButton;
            selectedTimeButton.setSelected(true);
            selectedTime = time;
        });

        // Add to appropriate grid
        if (time.contains("am")) {
            gridMorning.addView(timeButton);
        } else {
            gridAfternoon.addView(timeButton);
        }
    }
    String selectedDoctorName = "Dr. Maria Santos"; // <-- This needs to be fetched when the doctor is selected
    String selectedAppointmentName = "Prenatal Check-up";



    private void bookAppointment() {
        // Validate all fields
        if (selectedAppointmentType == null) {
            Toast.makeText(this, "Please select appointment type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDoctor == null || selectedDoctorId == null) {
            Toast.makeText(this, "Please select a doctor", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime == null) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Show the confirmation dialog instead of booking directly
        showConfirmationDialog();
    }
    private void executeBooking() {
        // Get current user ID from Firebase Auth
        String userId = null;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Check if the user is logged in
        if (userId == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            // Optionally redirect to login screen
            return;
        }

        // Use selectedDoctor's name and specialty (which is also the appointmentType)
        String doctorName = selectedDoctor.getName();
        String specialty = selectedDoctor.getSpecialty();

        // 2. Create the booking object using the currently selected data
        Map<String, Object> booking = new HashMap<>();
        booking.put("userId", userId);
        booking.put("doctorId", selectedDoctorId);
        booking.put("doctorName", doctorName); // <-- Fetched from selectedDoctor
        booking.put("specialty", specialty);   // <-- Fetched from selectedDoctor
        booking.put("date", selectedDate);
        booking.put("time", selectedTime);
        booking.put("status", "pending");

        // 3. Save to Firebase
        db.collection("appointments")
                .add(booking)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to book appointment", Toast.LENGTH_SHORT).show();
                });
    }

    private void showConfirmationDialog() {
        // Use AlertDialog Builder to create a custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // We'll create this layout next: dialog_appointment_confirm.xml
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_appointment_confirm, null);
        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();

        // Initialize dialog views
        TextView confirmAppointmentType = dialogView.findViewById(R.id.confirmAppointmentType);
        TextView confirmDoctorName = dialogView.findViewById(R.id.confirmDoctorName);
        TextView confirmDoctorSpecialty = dialogView.findViewById(R.id.confirmDoctorSpecialty);
        TextView confirmTime = dialogView.findViewById(R.id.confirmTime);
        TextView confirmDate = dialogView.findViewById(R.id.confirmDate);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);

        // Set data from activity variables
        confirmAppointmentType.setText(selectedAppointmentType);
        confirmDoctorName.setText(selectedDoctor.getName());
        confirmDoctorSpecialty.setText(selectedDoctor.getSpecialty());

        // Format date for display (e.g., 02 January 2026)
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            String formattedDate = displayFormat.format(dbFormat.parse(selectedDate));
            confirmDate.setText(formattedDate);
        } catch (Exception e) {
            confirmDate.setText(selectedDate); // Fallback
        }

        confirmTime.setText(selectedTime);

        // Confirmation button listener
        confirmButton.setOnClickListener(v -> {
            alertDialog.dismiss();
            executeBooking(); // Proceed with the actual booking
        });

        if (alertDialog.getWindow() != null) {
            // Apply the transparent background for a custom-shaped dialog
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        alertDialog.show();
    }

    // Appointment model class
    private static class Appointment {
        public String userId;
        public String doctorId;
        public String appointmentType;
        public String date;
        public String time;
        public String status;

        public Appointment(String userId, String doctorId, String appointmentType,
                           String date, String time, String status) {
            this.userId = userId;
            this.doctorId = doctorId;
            this.appointmentType = appointmentType;
            this.date = date;
            this.time = time;
            this.status = status;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure the adapter listener is removed when the activity is closed
        if (doctorAdapter != null) {
            doctorAdapter.removeListener();
        }
    }


}