package com.example.sttherese.patient.activities;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
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
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookingAppointmentActivity extends AppCompatActivity {

    private MaterialAutoCompleteTextView spinnerAppointmentType;
    private CardView doctorCard;
    private ImageView doctorImageView, closeButton;
    private TextView doctorNameText, doctorSpecialtyText;
    private Button buttonPickDate;
    private TextView textSelectedDate;
    private GridLayout gridMorning, gridAfternoon;
    private MaterialButton buttonBook;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth; // ADDED
    private String selectedDoctorId = null;
    private String selectedDate = null;
    private String selectedTime = null;
    private String selectedAppointmentType = null;
    private Doctor selectedDoctor = null; // ADDED

    private List<String> appointmentTypes = new ArrayList<>();
    private Button selectedTimeButton = null;

    // New class variable to hold the query for the adapter
    private Query doctorsQuery = null;
    private DoctorAdapter doctorAdapter; // Class variable for the adapter

    private String selectedCategorySchedule = null;
    private List<String> availableDaysForCategory = new ArrayList<>(); // Store all available days
    private String selectedSpecialty = null; // NEW: Track selected specialty
    private List<Map<String, Object>> servicesListWithSpecialty = new ArrayList<>(); // NEW


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_appointment);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance(); // INITIALIZE AUTH

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
        buttonBook.setOnClickListener(v -> checkPatientDailyAppointmentLimit());
    }

    private void fetchAppointmentTypes() {
        db.collection("specialties")
                .get()
                .addOnSuccessListener(specialtySnapshots -> {
                    servicesListWithSpecialty.clear();
                    appointmentTypes.clear();

                    if (specialtySnapshots.isEmpty()) {
                        Toast.makeText(this, "No specialties found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Track how many specialties we need to query
                    int totalSpecialties = specialtySnapshots.size();
                    final int[] processedCount = {0};

                    // Loop through each specialty
                    for (DocumentSnapshot specialtyDoc : specialtySnapshots) {
                        String specialtyId = specialtyDoc.getId(); // e.g., "OB-GYNE"
                        String specialtyName = specialtyDoc.getString("name");

                        // Query the services sub-collection
                        db.collection("specialties")
                                .document(specialtyId)
                                .collection("services")
                                .get()
                                .addOnSuccessListener(serviceSnapshots -> {
                                    // Add each service to the list
                                    for (DocumentSnapshot serviceDoc : serviceSnapshots) {
                                        String serviceName = serviceDoc.getString("name");

                                        if (serviceName != null) {
                                            // Store service name for dropdown
                                            appointmentTypes.add(serviceName);

                                            // Store service with its specialty for later lookup
                                            Map<String, Object> serviceData = new HashMap<>();
                                            serviceData.put("serviceName", serviceName);
                                            serviceData.put("specialty", specialtyId);
                                            serviceData.put("specialtyName", specialtyName);
                                            serviceData.put("serviceId", serviceDoc.getId());
                                            servicesListWithSpecialty.add(serviceData);
                                        }
                                    }

                                    // Check if all specialties have been processed
                                    processedCount[0]++;
                                    if (processedCount[0] == totalSpecialties) {
                                        // All done - setup the dropdown
                                        setupDropdownAdapter();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    processedCount[0]++;
                                    if (processedCount[0] == totalSpecialties) {
                                        setupDropdownAdapter();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load specialties: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupDropdownAdapter() {
        // Sort alphabetically for better UX
        Collections.sort(appointmentTypes);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                appointmentTypes
        );
        spinnerAppointmentType.setAdapter(adapter);
    }

    private void fetchDoctorsByType(String appointmentType) {
        // 1. Reset everything
        selectedDoctorId = null;
        selectedDoctor = null;
        selectedSpecialty = null;
        doctorsQuery = null;
        gridMorning.removeAllViews();
        gridAfternoon.removeAllViews();
        textSelectedDate.setText("No date selected");
        selectedDate = null;
        selectedTime = null;
        resetDoctorCardToDefault();

        // 2. Find the specialty for this service
        String specialty = null;
        for (Map<String, Object> serviceData : servicesListWithSpecialty) {
            if (appointmentType.equals(serviceData.get("serviceName"))) {
                specialty = (String) serviceData.get("specialty");
                selectedSpecialty = specialty;
                break;
            }
        }

        if (specialty == null) {
            Toast.makeText(this, "Could not determine specialty for this service.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Query ALL active doctors with this specialty
        doctorsQuery = db.collection("doctors")
                .whereEqualTo("specialty", specialty)
                .whereEqualTo("is_active", true);

        // 4. Prompt user to select a doctor
        promptDoctorSelectionCard(specialty);
        Toast.makeText(this, "Please select a doctor", Toast.LENGTH_LONG).show();
    }
    /**
     * Clears the current doctor's details and shows a default placeholder message.
     */
    private void resetDoctorCardToDefault() {
        // Inflate the default content for the doctor card
        View doctorView = LayoutInflater.from(this).inflate(R.layout.doctor_card_content, null);

        ImageView doctorImageView = doctorView.findViewById(R.id.doctorImage);
        TextView doctorNameText = doctorView.findViewById(R.id.doctorName);
        TextView doctorSpecialtyText = doctorView.findViewById(R.id.doctorSpecialty);

        // Set placeholder content
        doctorNameText.setText("Select Doctor");
        doctorSpecialtyText.setText("Tap to choose");
        doctorImageView.setImageResource(R.drawable.ic_doctor_placeholder); // Use your default image

        // Clear and update card view
        doctorCard.removeAllViews();
        doctorCard.addView(doctorView);
    }

    private void fetchClinicSchedulesForDoctor(String doctorId, String category) {
        // Query all schedule documents that start with the doctor ID
        db.collection("clinic_schedules")
                .orderBy(com.google.firebase.firestore.FieldPath.documentId())
                .startAt(doctorId)
                .endAt(doctorId + "\uf8ff")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    availableDaysForCategory.clear();

                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No clinic schedules found for this doctor.", Toast.LENGTH_SHORT).show();
                        // Still allow doctor selection, but warn user
                        fetchDoctorsForSelection(doctorId, category);
                        return;
                    }

                    // Build a list of available days from the schedules
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // Extract day from document ID (e.g., "D001_MON" -> need to convert MON to Monday)
                        String docId = doc.getId();
                        String[] parts = docId.split("_");

                        if (parts.length >= 2) {
                            String dayAbbr = parts[1]; // e.g., "MON", "TUE", "WED"
                            String dayOfWeek = convertDayAbbrToFull(dayAbbr);

                            if (dayOfWeek != null && !availableDaysForCategory.contains(dayOfWeek)) {
                                availableDaysForCategory.add(dayOfWeek);
                            }
                        }
                    }

                    // Create a readable schedule string
                    selectedCategorySchedule = formatScheduleDays(availableDaysForCategory);

                    // Now fetch and display the doctor
                    fetchDoctorsForSelection(doctorId, category);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load clinic schedules: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Still allow proceeding even if schedule fetch fails
                    fetchDoctorsForSelection(doctorId, category);
                });
    }


    private String convertDayAbbrToFull(String dayAbbr) {
        if (dayAbbr == null) return null;

        switch (dayAbbr.toUpperCase()) {
            case "MON": return "Monday";
            case "TUE": return "Tuesday";
            case "WED": return "Wednesday";
            case "THU": return "Thursday";
            case "FRI": return "Friday";
            case "SAT": return "Saturday";
            case "SUN": return "Sunday";
            default: return null;
        }
    }
    private String formatScheduleDays(List<String> days) {
        if (days.isEmpty()) return "No schedule available";

        // Define day order
        String[] dayOrder = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        // Sort days according to the week order
        List<String> sortedDays = new ArrayList<>();
        for (String day : dayOrder) {
            if (days.contains(day)) {
                sortedDays.add(day);
            }
        }

        // Check if it's consecutive days (Monday to Saturday)
        if (isConsecutiveDays(sortedDays, dayOrder)) {
            return sortedDays.get(0) + " to " + sortedDays.get(sortedDays.size() - 1);
        }

        // Otherwise, return comma-separated list
        return String.join(", ", sortedDays);
    }
    private boolean isConsecutiveDays(List<String> days, String[] dayOrder) {
        if (days.size() < 2) return false;

        int firstIndex = -1;
        int lastIndex = -1;

        for (int i = 0; i < dayOrder.length; i++) {
            if (dayOrder[i].equals(days.get(0))) {
                firstIndex = i;
            }
            if (dayOrder[i].equals(days.get(days.size() - 1))) {
                lastIndex = i;
            }
        }

        // Check if all days between first and last are included
        if (lastIndex - firstIndex + 1 == days.size()) {
            for (int i = firstIndex; i <= lastIndex; i++) {
                if (!days.contains(dayOrder[i])) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    // Rename and modify the function that was fetchAndDisplayDoctor
    private void fetchDoctorsForSelection(String doctorId, String category) {

        // 1. Set up the query for the Doctor Selection Dialog
        // This query lists ALL doctors matching the service's category/specialty.
        doctorsQuery = db.collection("doctors")
                .whereEqualTo("specialty", category);

        // 2. Clear any previously selected doctor state
        selectedDoctorId = null;
        selectedDoctor = null;

        // 3. Immediately update the UI to prompt the user to select
        promptDoctorSelectionCard(category); // <--- NEW HELPER METHOD

        // Inform the user they can now choose
        Toast.makeText(this, "Please tap the card to select a doctor.", Toast.LENGTH_LONG).show();
    }
    /**
     * Updates the Doctor Card UI to prompt the user to tap and select a doctor.
     * @param category The category/specialty the user should choose from.
     */
    private void promptDoctorSelectionCard(String category) {
        // Inflate a temporary view or use your existing doctor_card_content structure
        View doctorView = LayoutInflater.from(this).inflate(R.layout.doctor_card_content, null);

        // Assuming these IDs exist in doctor_card_content.xml:
        ImageView doctorImageView = doctorView.findViewById(R.id.doctorImage);
        TextView doctorNameText = doctorView.findViewById(R.id.doctorName);
        TextView doctorSpecialtyText = doctorView.findViewById(R.id.doctorSpecialty);

        // Set prompt content
        doctorNameText.setText("Select Your Doctor");
        doctorSpecialtyText.setText("Specialty: " + category);

        // Use a relevant icon instead of a doctor image, or your default placeholder
        doctorImageView.setImageResource(R.drawable.ic_doctor_placeholder);

        // Clear and update card view
        doctorCard.removeAllViews();
        doctorCard.addView(doctorView);
    }

    private void showDoctorSelectionDialog() {
        if (doctorsQuery == null) {
            Toast.makeText(this, "Please select appointment type first", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_doctor_selection, null);

        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();

        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewDoctors);
        Button buttonCloseDialog = dialogView.findViewById(R.id.buttonCloseDialog);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DoctorAdapter.OnDoctorClickListener listener = doctor -> {
            // Update selected doctor
            selectedDoctor = doctor;
            selectedDoctorId = doctor.getId();

            // IMPORTANT: Re-fetch clinic schedules for the newly selected doctor
            db.collection("clinic_schedules")
                    .orderBy(com.google.firebase.firestore.FieldPath.documentId())
                    .startAt(selectedDoctorId)
                    .endAt(selectedDoctorId + "\uf8ff")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        availableDaysForCategory.clear();

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String docId = doc.getId();
                            String[] parts = docId.split("_");

                            if (parts.length >= 2) {
                                String dayAbbr = parts[1]; // e.g., "MON", "TUE", "WED"
                                String dayOfWeek = convertDayAbbrToFull(dayAbbr);

                                if (dayOfWeek != null && !availableDaysForCategory.contains(dayOfWeek)) {
                                    availableDaysForCategory.add(dayOfWeek);
                                }
                            }
                        }

                        selectedCategorySchedule = formatScheduleDays(availableDaysForCategory);

                        // Update the doctor card
                        db.collection("doctors").document(selectedDoctorId).get()
                                .addOnSuccessListener(this::updateDoctorCard);
                    });

            // Clear date/time selection since doctor changed
            selectedDate = null;
            selectedTime = null;
            textSelectedDate.setText("No date selected");
            gridMorning.removeAllViews();
            gridAfternoon.removeAllViews();

            Toast.makeText(this, doctor.getName() + " selected.", Toast.LENGTH_SHORT).show();
            alertDialog.dismiss();
        };

        doctorAdapter = new DoctorAdapter(
                this,
                listener,
                doctorsQuery,
                R.layout.item_doctor_no_button
        );
        recyclerView.setAdapter(doctorAdapter);

        buttonCloseDialog.setOnClickListener(v -> alertDialog.dismiss());

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

    private boolean isDayAvailable(Calendar selectedCalendar, String categoryScheduleDays) {
        if (availableDaysForCategory.isEmpty()) {
            return false;
        }

        // Get the full day name (e.g., "Wednesday")
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.US);
        String selectedDayFull = dayFormat.format(selectedCalendar.getTime());

        // Check if the selected day is in the list of available days
        return availableDaysForCategory.contains(selectedDayFull);
    }

    private void blockUnavailableSlots(String doctorId, String date) {
        List<String> bookedTimes = new ArrayList<>();

        // Check existing appointments (bookings) ---
        db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(appointmentSnapshots -> {
                    for (DocumentSnapshot doc : appointmentSnapshots) {
                        // We only block slots that are 'pending' or 'confirmed'
                        String status = doc.getString("status");
                        if (status != null && (status.equals("pending") || status.equals("confirmed"))) {
                            String time = doc.getString("time");
                            if (time != null) {
                                bookedTimes.add(time);
                            }
                        }
                    }

                    // --- Step B: Check schedule exceptions (leaves/absences) ---
                    db.collection("schedule_exceptions")
                            .whereEqualTo("doctor_id", doctorId)
                            .whereEqualTo("date", date)
                            .get()
                            .addOnSuccessListener(exceptionSnapshots -> {
                                for (DocumentSnapshot doc : exceptionSnapshots) {
                                    String time = doc.getString("time");
                                    if (time != null) {
                                        // If time is set, block that specific slot
                                        bookedTimes.add(time);
                                    } else {
                                        // If time is null, assume the whole day is blocked/on leave
                                        disableAllTimeSlots(true);
                                        return;
                                    }
                                }

                                // --- Step C: Apply Blocks to UI ---
                                applyBlocksToUI(bookedTimes);

                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to check schedule exceptions.", Toast.LENGTH_SHORT).show();
                                applyBlocksToUI(bookedTimes); // Proceed with only appointments blocked
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check existing appointments.", Toast.LENGTH_SHORT).show();
                });
    }

    private void applyBlocksToUI(List<String> bookedTimes) {
        // Helper function for iterating and blocking
        Runnable blockChecker = () -> {
            // Iterate through Morning Grid
            for (int i = 0; i < gridMorning.getChildCount(); i++) {
                View child = gridMorning.getChildAt(i);
                if (child instanceof Button) {
                    Button button = (Button) child;
                    String time = button.getText().toString();

                    String originalTime = (button.getTag() instanceof String) ? (String)button.getTag() : time;

                    if (bookedTimes.contains(originalTime)) {
                        button.setEnabled(false);
                        button.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_unavailable));
                        button.setText("BOOKED");
                        button.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    }
                }
            }

            // Iterate through Afternoon Grid
            for (int i = 0; i < gridAfternoon.getChildCount(); i++) {
                View child = gridAfternoon.getChildAt(i);
                if (child instanceof Button) {
                    Button button = (Button) child;
                    String time = button.getText().toString();

                    String originalTime = (button.getTag() instanceof String) ? (String)button.getTag() : time;

                    if (bookedTimes.contains(originalTime)) {
                        button.setEnabled(false);
                        button.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_unavailable));
                        button.setText("BOOKED");
                        button.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    }
                }
            }
        };

        // Execute the checker
        blockChecker.run();
    }

    private void disableAllTimeSlots(boolean showToast) {
        // Disable all buttons in the morning grid
        for (int i = 0; i < gridMorning.getChildCount(); i++) {
            View child = gridMorning.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                button.setEnabled(false);
                button.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_unavailable));
            }
        }

        // Disable all buttons in the afternoon grid
        for (int i = 0; i < gridAfternoon.getChildCount(); i++) {
            View child = gridAfternoon.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                button.setEnabled(false);
                button.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_unavailable));
            }
        }

        if (showToast) {
            Toast.makeText(this, "The doctor is on leave or unavailable for the entire day.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateDoctorCard(DocumentSnapshot doctor) {
        // Inflate doctor card content
        View doctorView = LayoutInflater.from(this).inflate(R.layout.doctor_card_content, null);
        doctorImageView = doctorView.findViewById(R.id.doctorImage);
        doctorNameText = doctorView.findViewById(R.id.doctorName);
        doctorSpecialtyText = doctorView.findViewById(R.id.doctorSpecialty);

        // Set doctor info
        doctorNameText.setText(doctor.getString("name"));
        doctorSpecialtyText.setText(doctor.getString("specialty") + " | " + selectedCategorySchedule); // Added schedule info

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

        // Check if the category schedule was found
        if (selectedCategorySchedule == null || availableDaysForCategory.isEmpty()) {
//            Toast.makeText(this, "Clinic schedule not defined for the selected doctor/service.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    // --- Schedule Check Logic ---
                    if (!isDayAvailable(selectedCalendar, selectedCategorySchedule)) {
                        Toast.makeText(this, "Service is not available on that day based on clinic schedule.", Toast.LENGTH_LONG).show();

                        // Clear all time slots and reset selectedDate/Time
                        textSelectedDate.setText("No date selected");
                        selectedDate = null;
                        selectedTime = null; // Important to reset time
                        gridMorning.removeAllViews();
                        gridAfternoon.removeAllViews();

                        return;
                    }
                    // --- End Schedule Check Logic ---

                    // If the date IS available, proceed:
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = sdf.format(selectedCalendar.getTime());

                    SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                    textSelectedDate.setText(displayFormat.format(selectedCalendar.getTime()));

                    // Reset selected time when date changes (prevents booking stale time)
                    selectedTime = null;
                    if (selectedTimeButton != null) {
                        selectedTimeButton.setSelected(false);
                        selectedTimeButton.setBackgroundResource(R.drawable.time_slot_available); // Reset visual state
                        selectedTimeButton = null;
                    }

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

        String scheduleDocId;
        try {
            SimpleDateFormat dateDbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.setTime(dateDbFormat.parse(date));
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.US);
            String dayAbbr = dayFormat.format(selectedCalendar.getTime()).toUpperCase(Locale.US);
            scheduleDocId = doctorId + "_" + dayAbbr;
        } catch (Exception e) {
            Toast.makeText(this, "Error processing date for schedule ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Fetch the doctor's standard schedule from clinic_schedules
        db.collection("clinic_schedules")
                .document(scheduleDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String startTimeStr = documentSnapshot.getString("start_time"); // e.g., "9:00"
                        String endTimeStr = documentSnapshot.getString("end_time");     // e.g., "14:00"
                        Long slotDurationLong = documentSnapshot.getLong("slot_duration_minutes");

                        int slotDurationMinutes = (slotDurationLong != null) ? slotDurationLong.intValue() : 30; // Default to 30 mins if null

                        // 2. Generate slots based on the schedule
                        generateTimeSlots(startTimeStr, endTimeStr, slotDurationMinutes);

                        // 3. Block unavailable slots (appointments and exceptions)
                        blockUnavailableSlots(doctorId, selectedDate);
                    } else {
                        Toast.makeText(this, "No specific clinic schedule found for this day.", Toast.LENGTH_SHORT).show();
                        disableAllTimeSlots(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch schedule.", Toast.LENGTH_SHORT).show();
                    disableAllTimeSlots(false);
                });
    }
    private void generateTimeSlots(String startTimeStr, String endTimeStr, int slotDurationMinutes) {
        // Clear existing slots
        gridMorning.removeAllViews();
        gridAfternoon.removeAllViews();

        SimpleDateFormat timeFormat24 = new SimpleDateFormat("HH:mm", Locale.US);
        SimpleDateFormat timeFormat12 = new SimpleDateFormat("h:mm a", Locale.US);
        Calendar current = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        try {
            current.setTime(timeFormat24.parse(startTimeStr));
            end.setTime(timeFormat24.parse(endTimeStr));
        } catch (Exception e) {
            Toast.makeText(this, "Error parsing time format.", Toast.LENGTH_SHORT).show();
            return;
        }

        while (current.before(end)) {
            String slotTime24 = timeFormat24.format(current.getTime()); // Store 24-hour format for backend
            String slotTime12 = timeFormat12.format(current.getTime()); // Display 12-hour format
            createTimeSlotButton(slotTime24, slotTime12, current.get(Calendar.AM_PM));
            current.add(Calendar.MINUTE, slotDurationMinutes);
        }
    }


    private void createTimeSlotButton(String time24, String time12, int ampm) {
        Button button = new Button(this);

        // Display 12-hour format to user
        button.setText(time12);
        // Store 24-hour format as tag for backend operations
        button.setTag(time24);

        // Set background drawable
        button.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_available));
        button.setTextColor(Color.BLACK);

        // Remove default padding
        button.setPadding(16, 8, 16, 8);

        // Figma font
        try {
            Typeface poppinsSemiBold = ResourcesCompat.getFont(this, R.font.poppins_semi_bold);
            button.setTypeface(poppinsSemiBold);
        } catch (Exception e) {
            button.setTypeface(null, Typeface.BOLD);
        }

        // Make it not all caps (default Android behavior)
        button.setAllCaps(false);

        button.setOnClickListener(this::onTimeSlotClicked);

        // Layout params
        float density = getResources().getDisplayMetrics().density;
        int height = (int) (40 * density);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = height;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(6, 6, 6, 6);
        button.setLayoutParams(params);

        if (ampm == Calendar.AM) gridMorning.addView(button);
        else gridAfternoon.addView(button);
    }

    private void onTimeSlotClicked(View v) {
        Button button = (Button) v;

        if (selectedTimeButton != null && selectedTimeButton != button) {
            selectedTimeButton.setSelected(false);
            selectedTimeButton.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_available));
            selectedTimeButton.setTextColor(Color.BLACK);
        }

        if (button.isSelected()) {
            button.setSelected(false);
            button.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_available));
            button.setTextColor(Color.BLACK);
            selectedTime = null;
            selectedTimeButton = null;
        } else {
            button.setSelected(true);
            button.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_selected));
            button.setTextColor(Color.WHITE);

            selectedTime = button.getTag().toString();
            selectedTimeButton = button;
        }
    }

    private String generateAppointmentId() {
        // Simple way to get a unique number based on current time (in milliseconds)
        // You might want to format this for better readability or add a counter from Firestore/Remote Config.
        long timestamp = System.currentTimeMillis();
        // A unique ID using the last 6-8 digits of the timestamp
        String uniqueSuffix = String.valueOf(timestamp).substring(String.valueOf(timestamp).length() - 8);
        return "APPT_" + uniqueSuffix;
    }
    // ***************************************************************
    // *** NEW LOGIC: CHECK PATIENT DAILY APPOINTMENT LIMIT ***
    // ***************************************************************

    private void checkPatientDailyAppointmentLimit() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to book an appointment.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDoctorId == null || selectedDate == null || selectedTime == null || selectedAppointmentType == null) {
            Toast.makeText(this, "Please complete all booking details.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();

        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date and time.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure the book button is enabled before starting the check (to handle re-clicks after failure)
        buttonBook.setEnabled(true);

        db.collection("appointments")
                .whereEqualTo("userId", currentUserId) // Check appointments for THIS patient
                .whereEqualTo("date", selectedDate)       // ...on THIS date
                .whereIn("status", List.of("pending", "confirmed")) // Only check active statuses
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Patient already has an active appointment on this day

                        // Requirement: display “You already have an active appointment on this day.”
                        showActiveDialog();
                        buttonBook.setEnabled(false);
                        buttonBook.setEnabled(true); // Re-enable on failure
                        return;
                    }

                    // If no active appointments found, proceed with the actual booking logic
                    showConfirmationDialog();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check appointment limit. Please try again.", Toast.LENGTH_SHORT).show();
                    buttonBook.setEnabled(true); // Re-enable if check failed due to technical error
                });
    }

    private void performBooking() {
        if (selectedDoctorId == null || selectedDate == null || selectedTime == null || selectedAppointmentType == null) {
            Toast.makeText(this, "Please select a doctor, date, and time.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();

        // Disable button to prevent double submission
        buttonBook.setEnabled(false);

        // 1. Query the 'patients' collection to find the document
        // where the field 'userID' matches the current user's Auth UID.
        db.collection("patients")
                .whereEqualTo("userId", currentUserId)
                .limit(1) // Assuming only one patient document per user ID
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {


                        // Extract the Document ID (which is patient ID)

                        String customPatientId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // Proceed with Booking using the fetched ID
                        saveAppointmentToFirestore(currentUserId, customPatientId);

                    } else {
                        Toast.makeText(this, "Error: Patient document with Auth ID not found in patients collection.", Toast.LENGTH_LONG).show();
                        buttonBook.setEnabled(true); // Re-enable on failure
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to retrieve patient data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    buttonBook.setEnabled(true); // Re-enable on failure
                });
    }
    private void saveAppointmentToFirestore(String currentUserId, String customPatientId) {

        // Get specialty details
        String doctorSpecialty = selectedDoctor != null ? selectedDoctor.getSpecialty() : "Unknown Specialty";

        // Generate the unique appointment document ID
        String customDocumentId = generateAppointmentId();

        //  Create the appointment object
        Map<String, Object> appointment = new HashMap<>();
        appointment.put("userId", currentUserId);
        appointment.put("patientId", customPatientId);
        appointment.put("doctorId", selectedDoctorId);
        appointment.put("doctorName", selectedDoctor != null ? selectedDoctor.getName() : "Unknown Doctor");
        appointment.put("specialty", doctorSpecialty);
        appointment.put("appointmentType", selectedAppointmentType);
        appointment.put("date", selectedDate);
        appointment.put("time", selectedTime);
        appointment.put("timestamp", new java.util.Date());
        appointment.put("status", "pending");

        //  Save to Firestore
        db.collection("appointments")
                .document(customDocumentId)
                .set(appointment)
                .addOnSuccessListener(aVoid -> {
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    showFailureDialog();
                    buttonBook.setEnabled(true); // Re-enable on failure
                });
    }

    private void showSuccessDialog() {
        // Use AlertDialog Builder to create a custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Inflate the new layout: dialog_appointment_success.xml
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_appointment_success, null);
        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();

        // The dialog should automatically close and finish the activity after a delay
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false); // Prevent back button dismissal

        // Set a delay (e.g., 2000 milliseconds or 2 seconds) to automatically close
        dialogView.postDelayed(() -> {
            if (alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
            finish();
        }, 2000); // 2 second delay

        if (alertDialog.getWindow() != null) {
            // Apply the transparent background for a custom-shaped dialog
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        alertDialog.show();
    }

    private void showActiveDialog()
    {
// Use AlertDialog Builder to create a custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Inflate the new layout: dialog_appointment_success.xml
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_active_appointment, null);
        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();

        // Get the OK button from the layout
        Button buttonOk = dialogView.findViewById(R.id.buttonOk);

        // Set listener to dismiss the dialog
        buttonOk.setOnClickListener(v -> alertDialog.dismiss());

        // Allow user to dismiss by clicking outside or pressing back, but the button is primary
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.setCancelable(true);

        if (alertDialog.getWindow() != null) {
            // Apply the transparent background for a custom-shaped dialog
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        alertDialog.show();


    }

    private void showFailureDialog() {
        // Use AlertDialog Builder to create a custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // We will create this new layout next: dialog_booking_failure.xml
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_booking_failure, null);
        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();

        // Get the OK button from the layout
        Button buttonOk = dialogView.findViewById(R.id.buttonOk);

        // Set listener to dismiss the dialog
        buttonOk.setOnClickListener(v -> alertDialog.dismiss());

        // Allow user to dismiss by clicking outside or pressing back, but the button is primary
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.setCancelable(true);

        if (alertDialog.getWindow() != null) {
            // Apply the transparent background for a custom-shaped dialog
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        alertDialog.show();
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

        String time12Hour = convertTo12HourFormat(selectedTime);
        confirmTime.setText(time12Hour);



        // Confirmation button listener
        confirmButton.setOnClickListener(v -> {
            alertDialog.dismiss();
            performBooking(); // Proceed with the actual booking
        });

        if (alertDialog.getWindow() != null) {
            // Apply the transparent background for a custom-shaped dialog
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        alertDialog.show();
    }
    private String convertTo12HourFormat(String time24) {
        try {
            SimpleDateFormat timeFormat24 = new SimpleDateFormat("HH:mm", Locale.US);
            SimpleDateFormat timeFormat12 = new SimpleDateFormat("h:mm a", Locale.US);
            Date date = timeFormat24.parse(time24);
            return timeFormat12.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return time24; // Return original if parsing fails
        }
    }
}