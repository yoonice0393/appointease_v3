package com.example.sttherese.doctor;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sttherese.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class DoctorCalendarActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView tvUpcomingTitle, tvUpcomingDate, tvUpcomingTime, tvUpcomingPatient;
    private ImageView btnAdd;
    private LinearLayout layoutDataContent, btnHome, btnAppointment, btnCalendar, btnHistory;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String clinicDoctorId;

    // Color codes for different calendar events
    private int appointmentColor;
    private int scheduleColor;
    private int exceptionColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_calendar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize colors
        appointmentColor = ContextCompat.getColor(this, R.color.red_primary); // Red for appointments
        scheduleColor = ContextCompat.getColor(this, R.color.green_primary); // Green for regular schedule
        exceptionColor = ContextCompat.getColor(this, R.color.orange_primary); // Orange for exceptions

        initializeViews();
        setupClickListeners();

        // Load all calendar data
        loadCalendarData();
    }

    private void initializeViews() {
        calendarView = findViewById(R.id.calendarView);
        tvUpcomingTitle = findViewById(R.id.tvUpcomingTitle);
        tvUpcomingDate = findViewById(R.id.tvUpcomingDate);
        tvUpcomingTime = findViewById(R.id.tvUpcomingTime);
        tvUpcomingPatient = findViewById(R.id.tvUpcomingPatient);

        btnHome = findViewById(R.id.btnHome);
        btnAppointment = findViewById(R.id.btnAppointment);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnHistory = findViewById(R.id.btnHistory);
        btnAdd = findViewById(R.id.btnAdd);

        // Set up date selection listener
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            loadAppointmentsForDate(date);
        });
    }

    private void setupClickListeners() {
        btnHome.setOnClickListener(v -> startActivity(new Intent(this, DoctorHomeActivity.class)));
        btnAppointment.setOnClickListener(v -> startActivity(new Intent(this, DoctorAppointmentActivity.class)));
        btnCalendar.setOnClickListener(v -> Toast.makeText(this, "Already on Calendar", Toast.LENGTH_SHORT).show());
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, DoctorHistoryActivity.class)));
        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, AvailabilityScheduling.class)));

    }

    private void loadCalendarData() {
        String authId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (authId == null) {
            Toast.makeText(this, "Doctor not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("DoctorCalendar", "Attempting to find Doctor Clinic ID using Auth ID: " + authId);


        // STEP 1: Look up the Clinic ID (e.g., D001) in the 'doctors' collection
        db.collection("doctors")
                .whereEqualTo("user_id", authId) // Find the doctor profile linked to the current Auth ID
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Doctor profile not found. Cannot load schedule.", Toast.LENGTH_LONG).show();
                        android.util.Log.e("DoctorCalendar", "No doctor document found for Auth ID: " + authId);
                        return;
                    }

                    // The document ID (key) of the 'doctors' document is the Clinic ID (e.g., D001)
                    // which is needed for the 'appointments' collection.
                    String clinicId = querySnapshot.getDocuments().get(0).getId();
                    android.util.Log.d("DoctorCalendar", "Found Doctor Clinic ID: " + clinicId);

                    this.clinicDoctorId = clinicId;
                    // STEP 2: Use the Clinic ID to load all calendar data
                    loadUpcomingAppointments(clinicId);
                    loadRegularSchedule(clinicId);
                    loadScheduleExceptions(clinicId);

                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DoctorCalendar", "Failed to load doctor profile", e);
                    Toast.makeText(this, "Error fetching doctor ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUpcomingAppointments(String doctorId) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        android.util.Log.d("DoctorCalendar", "Querying appointments for doctorId: " + doctorId);

        db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .whereIn("status", List.of("pending", "confirmed"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    android.util.Log.d("DoctorCalendar", "Found " + querySnapshot.size() + " appointments");

                    HashSet<CalendarDay> appointmentDates = new HashSet<>();
                    long now = new Date().getTime();
                    long smallestFutureDifference = Long.MAX_VALUE;
                    DocumentSnapshot upcomingAppointmentDoc = null;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String dateStr = doc.getString("date");
                        String timeStr = doc.getString("time");

                        android.util.Log.d("DoctorCalendar", "Appointment: date=" + dateStr + ", time=" + timeStr);

                        if (dateStr == null || timeStr == null) continue;

                        try {
                            Date dateOnly = dateFormat.parse(dateStr);
                            org.threeten.bp.LocalDate localDate = convertToLocalDate(dateOnly);
                            appointmentDates.add(CalendarDay.from(localDate));

                            // Find next upcoming appointment (time is in 24-hour format like "09:30")
                            Date appointmentDateTime = dateTimeFormat.parse(dateStr + " " + timeStr);
                            long appointmentTimeMillis = appointmentDateTime.getTime();

                            if (appointmentTimeMillis >= now) {
                                long timeDifference = appointmentTimeMillis - now;
                                if (timeDifference < smallestFutureDifference) {
                                    smallestFutureDifference = timeDifference;
                                    upcomingAppointmentDoc = doc;
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("DoctorCalendar", "Error parsing date/time", e);
                            e.printStackTrace();
                        }
                    }

                    android.util.Log.d("DoctorCalendar", "Total appointment dates with dots: " + appointmentDates.size());

                    // Decorate calendar with appointment dates (Red dots)
                    calendarView.addDecorator(new EventDecorator(appointmentColor, appointmentDates));

                    // Display upcoming appointment
                    if (upcomingAppointmentDoc != null) {
                        displayUpcomingAppointment(upcomingAppointmentDoc);

                        // Highlight and scroll to the date
                        String latestDateStr = upcomingAppointmentDoc.getString("date");
                        if (latestDateStr != null) {
                            try {
                                Date latestDate = dateFormat.parse(latestDateStr);
                                org.threeten.bp.LocalDate localDate = convertToLocalDate(latestDate);
                                CalendarDay latestCalendarDay = CalendarDay.from(localDate);
                                calendarView.setSelectedDate(latestCalendarDay);
                                calendarView.setCurrentDate(latestCalendarDay);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        displayNoUpcomingAppointment();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DoctorCalendar", "Failed to load appointments", e);
                    Toast.makeText(this, "Failed to load appointments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void loadRegularSchedule(String doctorId) {
        // Load doctor's regular weekly schedule from clinic_schedules collection
        db.collection("clinic_schedules")
                .whereEqualTo("doctor_id", doctorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        List<String> workingDays = new ArrayList<>();

                        // Collect all working days from schedule documents
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String dayOfWeek = doc.getString("day_of_week");
                            if (dayOfWeek != null && !workingDays.contains(dayOfWeek)) {
                                workingDays.add(dayOfWeek);
                            }
                        }

                        if (!workingDays.isEmpty()) {
                            HashSet<CalendarDay> scheduleDates = generateScheduleDates(workingDays);
                            // Decorate calendar with regular schedule (Green dots)
                            calendarView.addDecorator(new EventDecorator(scheduleColor, scheduleDates));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load schedule: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void loadScheduleExceptions(String doctorId) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        db.collection("schedule_exceptions")
                .whereEqualTo("doctor_id", doctorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    HashSet<CalendarDay> exceptionDates = new HashSet<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String dateStr = doc.getString("date");
                        Boolean isAllDay = doc.getBoolean("is_all_day");

                        if (dateStr != null) {
                            try {
                                Date date = dateFormat.parse(dateStr);
                                org.threeten.bp.LocalDate localDate = convertToLocalDate(date);
                                exceptionDates.add(CalendarDay.from(localDate));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Decorate calendar with exception dates (Orange dots)
                    calendarView.addDecorator(new EventDecorator(exceptionColor, exceptionDates));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load exceptions", Toast.LENGTH_SHORT).show();
                });
    }

    private HashSet<CalendarDay> generateScheduleDates(List<String> workingDays) {
        HashSet<CalendarDay> scheduleDates = new HashSet<>();
        Calendar calendar = Calendar.getInstance();

        // Generate dates for the next 3 months
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.MONTH, 3);

        while (calendar.before(endDate)) {
            String dayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.getTime());

            if (workingDays.contains(dayOfWeek)) {
                org.threeten.bp.LocalDate localDate = convertToLocalDate(calendar.getTime());
                scheduleDates.add(CalendarDay.from(localDate));
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return scheduleDates;
    }

    private void loadAppointmentsForDate(CalendarDay date) {
        // 1. Use the stored Clinic ID
        String doctorId = this.clinicDoctorId;

        // 2. Check if the Clinic ID was successfully loaded
        if (doctorId == null) {
            Toast.makeText(this, "Doctor ID not loaded. Please try refreshing.", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                date.getYear(), date.getMonth(), date.getDay());

        db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("date", dateStr)
                .whereIn("status", List.of("pending", "confirmed"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    StringBuilder appointments = new StringBuilder();
                    if (!querySnapshot.isEmpty()) {

//                        appointments.append("Appointments for ").append(dateStr).append(":\n\n");

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String time = doc.getString("time");
                            String patientId = doc.getString("userId");
                            String appointmentType = doc.getString("appointmentType");

                            // Format time from 24-hour to 12-hour
                            String displayTime = time;
                            try {
                                SimpleDateFormat input = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                SimpleDateFormat output = new SimpleDateFormat("h:mm a", Locale.getDefault());
                                displayTime = output.format(input.parse(time));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            appointments.append("• ").append(displayTime)
                                    .append(" - ").append(appointmentType != null ? appointmentType : "Appointment")
                                    .append("\n");
                        }
                    }
                        // Call the dialog function here
                        showAppointmentsDialog(dateStr, appointments);
                    })
        .addOnFailureListener(e -> {
                        // Log error
                        Toast.makeText(this, "Error loading appointments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
                }

    private void showAppointmentsDialog(String dateStr, StringBuilder appointments) {
        // Check if there are appointments to show
        String content = appointments.length() > 0
                ? appointments.toString()
                : "No appointments or scheduled events for this date.";

        // 1. Create the custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_day_appointments); // Use the new layout name
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // 2. Set views
        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        TextView tvAppointmentList = dialog.findViewById(R.id.tvAppointmentList);
        Button btnClose = dialog.findViewById(R.id.btnClose);

        // 3. Populate data
        dialogTitle.setText("Appointments for " + dateStr);
        tvAppointmentList.setText(content);

        // Set text alignment based on content
        if (appointments.length() == 0) {
            tvAppointmentList.setGravity(android.view.Gravity.CENTER);
        } else {
            tvAppointmentList.setGravity(android.view.Gravity.START);
        }

        // 4. Set click listener for the close button
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // 5. Show the dialog
        dialog.show();
    }

    private void displayUpcomingAppointment(DocumentSnapshot appointmentDoc) {
        String doctorName = appointmentDoc.getString("doctorName");
        String dateStr = appointmentDoc.getString("date");
        String timeStr = appointmentDoc.getString("time");
        String appointmentType = appointmentDoc.getString("appointmentType");
        String userId = appointmentDoc.getString("userId");

        String formattedDate = dateStr;
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
            formattedDate = displayFormat.format(dbFormat.parse(dateStr));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Format time from 24-hour to 12-hour
        String formattedTime = timeStr;
        try {
            SimpleDateFormat input = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("h:mm a", Locale.getDefault());
            formattedTime = output.format(input.parse(timeStr));
        } catch (Exception e) {
            e.printStackTrace();
        }

        tvUpcomingTitle.setText("Next Appointment");
        tvUpcomingPatient.setText(appointmentType != null ? appointmentType : "Appointment");
        tvUpcomingDate.setText(formattedDate);
        tvUpcomingTime.setText(formattedTime != null ? formattedTime : "N/A");

        // Optional: Load patient name from users collection
        if (userId != null) {
            loadPatientName(userId);
        }
    }

    private void loadPatientName(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        if (firstName != null || lastName != null) {
                            String fullName = (firstName != null ? firstName : "") + " " +
                                    (lastName != null ? lastName : "");
                            tvUpcomingPatient.setText(fullName.trim());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep showing appointment type if patient name cannot be loaded
                });
    }

    private void displayNoUpcomingAppointment() {
        tvUpcomingTitle.setText("No Upcoming Appointments");
        tvUpcomingPatient.setText("Your schedule is clear");
        tvUpcomingDate.setText("");
        tvUpcomingTime.setText("");
    }

    private org.threeten.bp.LocalDate convertToLocalDate(Date date) {
        if (date == null) return null;
        return org.threeten.bp.Instant
                .ofEpochMilli(date.getTime())
                .atZone(org.threeten.bp.ZoneId.systemDefault())
                .toLocalDate();
    }

    // Custom Decorator for calendar events
    private class EventDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> dates;

        public EventDecorator(int color, Collection<CalendarDay> dates) {
            this.color = color;
            this.dates = new HashSet<>(dates);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(8, color));
        }
    }
}