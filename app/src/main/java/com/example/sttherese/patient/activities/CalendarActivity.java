package com.example.sttherese.patient.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView tvDoctorName, tvDoctorSpecialty, tvAppointmentDate, tvAppointmentTime, tvAppointmentType;
    private LinearLayout btnHome, btnDoctor, btnCalendar, btnHistory;
    private ImageView btnAdd;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        fetchAllUpcomingAppointments();
    }

    private void initializeViews() {
        calendarView = findViewById(R.id.calendarView);
        tvDoctorName = findViewById(R.id.tvDoctorName);
        tvDoctorSpecialty = findViewById(R.id.tvDoctorSpecialty);
        tvAppointmentDate = findViewById(R.id.tvAppointmentDate);
        tvAppointmentTime = findViewById(R.id.tvAppointmentTime);
        tvAppointmentType = findViewById(R.id.tvAppointmentType);
//        btnNavigate = findViewById(R.id.btnNavigate);

        // Bottom Navigation
        btnHome = findViewById(R.id.btnHome);
        btnDoctor = findViewById(R.id.btnDoctor);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnHistory = findViewById(R.id.btnHistory);
        btnAdd = findViewById(R.id.btnAdd);

        // Set listener for date selection
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            loadAppointmentsForDate(date);
        });
    }

    private void setupClickListeners() {
        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(CalendarActivity.this, Home.class));
        });

        btnDoctor.setOnClickListener(v -> {
            startActivity(new Intent(CalendarActivity.this, DoctorsActivity.class));
        });

        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(CalendarActivity.this, BookingAppointmentActivity.class));
            // Navigate to AddAppointmentActivity when ready
        });

        btnCalendar.setOnClickListener(v -> {
            showToast("Already at Calendar");
        });

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(CalendarActivity.this, HistoryActivity.class));
        });
//
//        btnNavigate.setOnClickListener(v -> {
//            // Navigate to appointment details or doctor profile
//            showToast("View Appointment Details");
//        });
    }

    private void fetchAllUpcomingAppointments() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            displayNoAppointment();
            return;
        }

        android.util.Log.d("PatientCalendar", "Loading appointments for userId: " + userId);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        db.collection("appointments")
                .whereEqualTo("userId", userId)
                .whereIn("status", List.of("pending", "confirmed"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    android.util.Log.d("PatientCalendar", "Found " + querySnapshot.size() + " appointments");

                    HashSet<CalendarDay> appointmentDates = new HashSet<>();
                    long now = new Date().getTime();
                    long smallestFutureDifference = Long.MAX_VALUE;
                    DocumentSnapshot upcomingAppointmentDoc = null;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String dateStr = doc.getString("date");
                        String timeStr = doc.getString("time");

                        if (dateStr == null || timeStr == null) continue;

                        try {
                            Date dateOnly = dateFormat.parse(dateStr);
                            org.threeten.bp.LocalDate localDate = convertToLocalDate(dateOnly);
                            appointmentDates.add(CalendarDay.from(localDate));

                            // Find next upcoming appointment (time in 24-hour format)
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
                            android.util.Log.e("PatientCalendar", "Error parsing date/time", e);
                            e.printStackTrace();
                        }
                    }

                    // Apply red dots to calendar for appointment dates
                    int dotColor = ContextCompat.getColor(CalendarActivity.this, R.color.red_primary);
                    calendarView.addDecorator(new EventDecorator(dotColor, appointmentDates));

                    // Display upcoming appointment
                    if (upcomingAppointmentDoc != null) {
                        displayAppointment(upcomingAppointmentDoc);

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
                        displayNoAppointment();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PatientCalendar", "Failed to load appointments", e);
                    Toast.makeText(this, "Failed to load appointments.", Toast.LENGTH_SHORT).show();
                    displayNoAppointment();
                });
    }

    private void loadAppointmentsForDate(CalendarDay date) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        // 1. Date string format for Firestore query (e.g., "2025-01-17")
        String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                date.getYear(), date.getMonth(), date.getDay());

        // 2. Date string format for the Dialog Header (e.g., "January 17, 2025")
        String displayHeaderDate = dateStr; // Initialize with raw string as fallback
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            // Use the format that matches your dialog image: "Month Day, Year"
            SimpleDateFormat headerFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            displayHeaderDate = headerFormat.format(dbFormat.parse(dateStr));
        } catch (Exception e) {
            // If parsing fails, displayHeaderDate remains the raw dateStr
            android.util.Log.e("CalendarActivity", "Error formatting header date", e);
        }

        String finalDisplayHeaderDate = displayHeaderDate;
        db.collection("appointments")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", dateStr)
                .whereIn("status", List.of("pending", "confirmed"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AppointmentDetail> appointments = new java.util.ArrayList<>();

                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String time = doc.getString("time");
                            String appointmentType = doc.getString("appointmentType");
                            String doctorName = doc.getString("doctorName");

                            // Format time from 24-hour to 12-hour
                            String displayTime = time;
                            try {
                                SimpleDateFormat input = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                SimpleDateFormat output = new SimpleDateFormat("h:mm a", Locale.getDefault());
                                displayTime = output.format(input.parse(time));
                            } catch (Exception e) {
                                android.util.Log.e("CalendarActivity", "Error formatting appointment time", e);
                            }

                            AppointmentDetail detail = new AppointmentDetail(
                                    displayTime,
                                    appointmentType != null ? appointmentType : "Appointment",
                                    doctorName != null ?  doctorName : "Doctor N/A"
                            );
                            appointments.add(detail);
                        }
                    }

                    showPatientAppointmentsDialog(finalDisplayHeaderDate, appointments);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    public static class AppointmentDetail {
        String time;
        String type;
        String doctor;

        public AppointmentDetail(String time, String type, String doctor) {
            this.time = time;
            this.type = type;
            this.doctor = doctor;
        }
    }
    private void showPatientAppointmentsDialog(String dateStr, List<AppointmentDetail> appointments) {
        // 1. Create the custom dialog
        final Dialog dialog = new Dialog(this);
        // Ensure you are using the new XML file name for the dialog
        dialog.setContentView(R.layout.dialog_patient_appointments);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        // Remove default dialog padding/insets
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // 2. Find views
        TextView dialogDate = dialog.findViewById(R.id.dialogDate); // The "January 17, 2025" text
        LinearLayout appointmentsContainer = dialog.findViewById(R.id.appointmentsContainer); // The dynamic list
        Button btnClose = dialog.findViewById(R.id.btnClose);

        // 3. Populate data
        // Assuming dateStr is already in the format "Month Day, Year" like "January 17, 2025"
        dialogDate.setText(dateStr);

        // Clear existing views just in case (optional)
        appointmentsContainer.removeAllViews();

        // Use LayoutInflater to create views from the appointment_card.xml
        LayoutInflater inflater = LayoutInflater.from(this);

        if (appointments != null && !appointments.isEmpty()) {
            for (AppointmentDetail appointment : appointments) {
                // Inflate the individual appointment card layout (see XML section below)
                View appointmentCardView = inflater.inflate(R.layout.card_patient_appointments, appointmentsContainer, false);

                TextView tvTime = appointmentCardView.findViewById(R.id.tvAppointmentTime);
                TextView tvType = appointmentCardView.findViewById(R.id.tvAppointmentType);
                TextView tvDoctor = appointmentCardView.findViewById(R.id.tvDoctorName);

                tvTime.setText(appointment.time);
                tvType.setText(appointment.type);
                tvDoctor.setText(appointment.doctor);

                // Add the inflated card to the container
                appointmentsContainer.addView(appointmentCardView);
            }
        } else {
            // Handle case where there are no appointments (e.g., show a placeholder)
            TextView noAppointmentsText = new TextView(this);
            noAppointmentsText.setText("You have no confirmed appointments for this date.");
            noAppointmentsText.setPadding(32, 32, 32, 32);
            // ... set text color and gravity
            appointmentsContainer.addView(noAppointmentsText);
        }

        // 4. Set click listener for the close button
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // 5. Show the dialog
        dialog.show();
    }

    private void displayAppointment(DocumentSnapshot appointmentDoc) {
        String docName = appointmentDoc.getString("doctorName");
        String dateStr = appointmentDoc.getString("date");
        String timeStr = appointmentDoc.getString("time");
        String appointmentType = appointmentDoc.getString("appointmentType");
        String doctorId = appointmentDoc.getString("doctorId");

        // Format date
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

        tvDoctorName.setText(docName != null ? docName : "N/A");
        tvAppointmentType.setText(appointmentType != null ? appointmentType : "Appointment");
        tvAppointmentDate.setText(formattedDate);
        tvAppointmentTime.setText(formattedTime);

        // Load doctor specialty from doctors collection
        if (doctorId != null) {
            loadDoctorSpecialty(doctorId);
        } else {
            tvDoctorSpecialty.setText("Specialist");
        }
    }

    private void loadDoctorSpecialty(String doctorId) {
        db.collection("doctors").document(doctorId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String specialty = documentSnapshot.getString("specialty");
                        tvDoctorSpecialty.setText(specialty != null ? specialty : "Specialist");
                    }
                })
                .addOnFailureListener(e -> {
                    tvDoctorSpecialty.setText("Specialist");
                });
    }

    private void displayNoAppointment() {
        tvDoctorName.setText("No Upcoming Appointments");
        tvDoctorSpecialty.setText("Check back later!");
        tvAppointmentType.setText("");
        tvAppointmentDate.setText("");
        tvAppointmentTime.setText("");
    }

    private org.threeten.bp.LocalDate convertToLocalDate(Date date) {
        if (date == null) return null;
        return org.threeten.bp.Instant
                .ofEpochMilli(date.getTime())
                .atZone(org.threeten.bp.ZoneId.systemDefault())
                .toLocalDate();
    }

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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}