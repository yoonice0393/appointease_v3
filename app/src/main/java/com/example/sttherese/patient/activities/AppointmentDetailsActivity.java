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
import androidx.core.content.ContextCompat;
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
        db.collection("services")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    appointmentTypes.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String type = document.getString("service_name");
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



    private String selectedCategorySchedule = null;
    private List<String> availableDaysForCategory = new ArrayList<>(); // Store all available days
    private void fetchDoctorsByType(String appointmentType) {
        // 1. Reset everything related to the previous doctor selection
        selectedDoctorId = null;
        selectedDoctor = null;
        doctorsQuery = null;
        gridMorning.removeAllViews();
        gridAfternoon.removeAllViews();
        textSelectedDate.setText("No date selected");

        // Clear and reset the Doctor Card UI to its default state
        resetDoctorCardToDefault(); // <-- NEW HELPER METHOD (see below)

        // Step 1: Get the service document to find assigned_doctor_id
        db.collection("services")
                .whereEqualTo("service_name", appointmentType)
                .limit(1)
                .get()
                .addOnSuccessListener(serviceQuerySnapshot -> {
                    if (!serviceQuerySnapshot.isEmpty()) {
                        DocumentSnapshot serviceDoc = serviceQuerySnapshot.getDocuments().get(0);
                        String assignedDoctorId = serviceDoc.getString("assigned_doctor_id"); // e.g., "D001"
                        String category = serviceDoc.getString("category"); // e.g., "Ob-gyne"

                        if (assignedDoctorId != null && category != null) {
                            // Success: Proceed to fetch schedules and doctor details
                            fetchClinicSchedulesForDoctor(assignedDoctorId, category);
                        } else {
                            Toast.makeText(this, "No Doctor Available in this service.", Toast.LENGTH_SHORT).show();
                            // State already reset above, now just exit.
                        }
                    } else {
                        Toast.makeText(this, "Service not found.", Toast.LENGTH_SHORT).show();
                        // State already reset above, now just exit.
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load service: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // State already reset above, now just exit.
                });
    }
    /**
     * Clears the current doctor's details and shows a default placeholder message.
     */
    private void resetDoctorCardToDefault() {
        // You should ensure you have a layout resource for the default/empty state
        // Let's assume you have a layout named 'doctor_card_placeholder' or you manually set the TextViews.

        // Manual reset if you don't have a separate placeholder layout:
        View doctorView = LayoutInflater.from(this).inflate(R.layout.doctor_card_content_empty, null);

        // Assuming these IDs exist in doctor_card_content.xml:
        ImageView doctorImageView = doctorView.findViewById(R.id.doctorImage);
        TextView doctorNameText = doctorView.findViewById(R.id.doctorName);
        TextView doctorSpecialtyText = doctorView.findViewById(R.id.doctorSpecialty);

        // Set placeholder content
        doctorNameText.setText("No Doctor Available");
        doctorSpecialtyText.setText("Select an Appointment Type");
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

        // We don't fetch the single doctor by ID anymore.

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

    private DoctorAdapter doctorAdapter; // Class variable for the adapter

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
                    .whereEqualTo("doctor_id", selectedDoctorId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        availableDaysForCategory.clear();

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String dayOfWeek = doc.getString("day_of_week");
                            if (dayOfWeek != null) {
                                availableDaysForCategory.add(dayOfWeek);
                            }
                        }

                        selectedCategorySchedule = formatScheduleDays(availableDaysForCategory);

                        // Update the doctor card
                        db.collection("doctors").document(selectedDoctorId).get()
                                .addOnSuccessListener(doc -> updateDoctorCard(doc));
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
            alertDialog.getWindow().setBackgroundDrawableResource(R.color.bg_color);
        }

        alertDialog.show();
    }

    // The isDayAvailable method stays the same - it already handles the schedule checking correctly
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

    // Inside AppointmentDetailsActivity

    private void blockUnavailableSlots(String doctorId, String date) {
        List<String> bookedTimes = new ArrayList<>();

        // --- Step A: Check existing appointments (bookings) ---
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
                                        // In this case, we disable all buttons and show a toast.
                                        disableAllTimeSlots(true);
                                        return;
                                    }
                                }

                                // --- Step C: Apply Blocks to UI ---
                                applyBlocksToUI(bookedTimes);

                            })
                            .addOnFailureListener(e -> {
                                // Log or handle exception fetch failure
                                Toast.makeText(this, "Failed to check schedule exceptions.", Toast.LENGTH_SHORT).show();
                                applyBlocksToUI(bookedTimes); // Proceed with only appointments blocked
                            });
                })
                .addOnFailureListener(e -> {
                    // Log or handle appointments fetch failure
                    Toast.makeText(this, "Failed to check existing appointments.", Toast.LENGTH_SHORT).show();
                });
    }


    // Inside AppointmentDetailsActivity

    // Modified applyBlocksToUI method
    private void applyBlocksToUI(List<String> bookedTimes) {
        // Helper function for iterating and blocking
        Runnable blockChecker = () -> {
            // Iterate through Morning Grid
            for (int i = 0; i < gridMorning.getChildCount(); i++) {
                View child = gridMorning.getChildAt(i);
                if (child instanceof Button) {
                    Button button = (Button) child;
                    String time = button.getText().toString();

                    // Retrieve the original time, or use the current text if original time is not tagged
                    String originalTime = (button.getTag() instanceof String) ? (String)button.getTag() : time;

                    if (bookedTimes.contains(originalTime)) {
                        button.setEnabled(false);
                        button.setBackgroundResource(R.drawable.time_slot_unavailable);
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
                        button.setBackgroundResource(R.drawable.time_slot_unavailable);
                        button.setText("BOOKED"); // <--- ADDED TEXT
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
                button.setBackgroundResource(R.drawable.time_slot_unavailable);
            }
        }

        // Disable all buttons in the afternoon grid
        for (int i = 0; i < gridAfternoon.getChildCount(); i++) {
            View child = gridAfternoon.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                button.setEnabled(false);
                button.setBackgroundResource(R.drawable.time_slot_unavailable);
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

        // Check if the category schedule was found
        if (selectedCategorySchedule == null) {
            Toast.makeText(this, "Clinic schedule not defined for this service.", Toast.LENGTH_SHORT).show();
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

                        // 🌟 FIX: Clear all time slots and reset selectedDate/Time
                        textSelectedDate.setText("No date selected");
                        selectedDate = null;
                        selectedTime = null; // Important to reset time
                        gridMorning.removeAllViews(); // <--- ADDED
                        gridAfternoon.removeAllViews(); // <--- ADDED
                        // 🌟 END FIX

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

        // Convert the selected date to a day of the week (e.g., "WED" for Wednesday)
        String dayOfWeekCode;
        try {
            SimpleDateFormat dateDbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateDbFormat.parse(date));

            // Get day of the week abbreviation (e.g., Mon, Tue, Wed...)
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.US);
            String dayAbbr = dayFormat.format(calendar.getTime()).toUpperCase(Locale.US); // e.g., "WED"

            // The Firestore document IDs use 'D001_WED', so we need the full ID part
            dayOfWeekCode = dayAbbr;

        } catch (Exception e) {
            Toast.makeText(this, "Error processing date.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construct the schedule document ID, e.g., "D001_WED"
//        String scheduleDocId = doctorId + "_" + dayOfWeekCode;

        // Construct the schedule document ID, e.g., "D001_WED"
        Calendar selectedCalendar = Calendar.getInstance();
        String scheduleDocId;
        try {
            SimpleDateFormat dateDbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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
                        generateTimeSlotsFromSchedule(startTimeStr, endTimeStr, slotDurationMinutes);

                        // 3. Block off slots based on existing appointments and exceptions
                        blockUnavailableSlots(doctorId, date);

                    } else {
                        // No specific schedule found for this doctor/day
                        Toast.makeText(this, "No specific clinic schedule found for this day.", Toast.LENGTH_SHORT).show();
                        // Fallback to the hardcoded default slots
                        createDefaultTimeSlots();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load schedule: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Fallback to the hardcoded default slots on failure
                    createDefaultTimeSlots();
                });
    }


    /**
     * Generates time slot buttons based on the doctor's daily schedule.
     * @param startTimeStr The start time (e.g., "9:00").
     * @param endTimeStr The end time (e.g., "14:00").
     * @param slotDurationMinutes The duration of each slot in minutes (e.g., 30).
     */
    private void generateTimeSlotsFromSchedule(String startTimeStr, String endTimeStr, int slotDurationMinutes) {
        if (startTimeStr == null || endTimeStr == null) return;

        try {
            SimpleDateFormat dbTimeFormat = new SimpleDateFormat("H:mm", Locale.getDefault()); // e.g., 9:00, 14:00
            SimpleDateFormat displayTimeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault()); // e.g., 9:00 am, 2:00 pm

            Calendar currentSlot = Calendar.getInstance();
            currentSlot.setTime(dbTimeFormat.parse(startTimeStr));

            Calendar endTime = Calendar.getInstance();
            endTime.setTime(dbTimeFormat.parse(endTimeStr));

            // Ensure we only look at the time component by setting a fixed date/time for comparison
            currentSlot.set(Calendar.YEAR, 2000);
            currentSlot.set(Calendar.MONTH, 0);
            currentSlot.set(Calendar.DAY_OF_MONTH, 1);
            endTime.set(Calendar.YEAR, 2000);
            endTime.set(Calendar.MONTH, 0);
            endTime.set(Calendar.DAY_OF_MONTH, 1);

            // Generate slots until the current time reaches the end time
            while (currentSlot.before(endTime)) {
                String timeDisplay = displayTimeFormat.format(currentSlot.getTime());

                // 🌟 FIX: Determine if it's morning based on the 24-hour clock
                // HOUR_OF_DAY returns 0-23 (0=midnight, 12=noon)
                boolean isMorning = currentSlot.get(Calendar.HOUR_OF_DAY) < 12; // <--- ADDED

                // Add the button, passing the new flag
                addTimeSlotButton(timeDisplay, true, isMorning); // <--- MODIFIED

                // Move to the next slot
                currentSlot.add(Calendar.MINUTE, slotDurationMinutes);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error generating time slots: " + e.getMessage(), Toast.LENGTH_LONG).show();
            createDefaultTimeSlots(); // Fallback
        }
    }

    private void createDefaultTimeSlots() {
        // Morning slots
        String[] morningSlots = {"9:00 am", "9:30 am", "10:00 am", "10:30 am", "11:00 am", "11:30 am"};
        for (String time : morningSlots) {
            addTimeSlotButton(time, true, true); // <--- ADDED 'true' for morning
        }

        // Afternoon slots
        String[] afternoonSlots = {"1:00 pm", "1:30 pm", "2:00 pm", "2:30 pm", "3:00 pm", "3:30 pm", "4:00 pm", "4:30 pm", "5:00 pm"};
        for (String time : afternoonSlots) {
            addTimeSlotButton(time, true, false); // <--- ADDED 'false' for afternoon
        }
    }

    // BEFORE: private void addTimeSlotButton(String time, boolean available) {
    private void addTimeSlotButton(String time, boolean available, boolean isMorning) { // <--- MODIFIED
        Button timeButton = new Button(this);
        timeButton.setText(time);
        timeButton.setEnabled(available);

        // Apply custom style
        if (available) {
            timeButton.setBackgroundResource(R.drawable.time_slot_selector);
        } else {
            timeButton.setBackgroundResource(R.drawable.time_slot_unavailable);
            timeButton.setText("UNAVAILABLE");
            timeButton.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

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

        // Add to appropriate grid using the flag
        if (isMorning) {
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