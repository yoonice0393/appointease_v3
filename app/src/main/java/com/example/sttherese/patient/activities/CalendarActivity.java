package com.example.sttherese.patient.activities;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;


public class CalendarActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView doctorName, appointmentDate, appointmentTime;
    // Bottom Navigation
    private LinearLayout btnHome, btnDoctor, btnCalendar, btnHistory;
    private ImageView btnAdd;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        calendarView = findViewById(R.id.calendarView);
        doctorName = findViewById(R.id.tvDoctorName);
        appointmentDate = findViewById(R.id.tvAppointmentDate);
        appointmentTime = findViewById(R.id.tvAppointmentTime);

        mAuth = FirebaseAuth.getInstance(); // <-- NEW
        db = FirebaseFirestore.getInstance(); // <-- NEW

        // Set listener for date selection
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            // You can add logic here to fetch appointments for the selected date
            String selectedDate = date.getDay() + " " + date.getMonth() + " " + date.getYear();
        });

        initializeViews();
        setupClickListeners();

        fetchAllUpcomingAppointments(); // ✅ This will show dots on calendar

    }

    private void initializeViews() {




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

    private org.threeten.bp.LocalDate convertToLocalDate(java.util.Date date) {
        if (date == null) return null;

        // Convert Date to Instant, then to ZoneId, then to LocalDate
        return org.threeten.bp.Instant
                .ofEpochMilli(date.getTime())
                .atZone(org.threeten.bp.ZoneId.systemDefault())
                .toLocalDate();
    }

    private void fetchAllUpcomingAppointments() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            displayNoAppointment();
            return;
        }

        db.collection("appointments")
                .whereEqualTo("userId", userId)
                .whereIn("status", List.of("pending", "confirmed"))
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault());

                    HashSet<CalendarDay> appointmentDates = new HashSet<>();

                    long now = new Date().getTime();
                    long smallestFutureDifference = Long.MAX_VALUE;
                    DocumentSnapshot upcomingAppointmentDoc = null;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String dateStr = doc.getString("date"); // e.g., "2025-12-25"
                        String timeStr = doc.getString("time"); // e.g., "9:00 am"

                        if (dateStr == null || timeStr == null) continue;

                        try {
                            Date dateOnly = dateFormat.parse(dateStr);

                            // 1. Logic for Calendar Decoration
                            org.threeten.bp.LocalDate localDate = convertToLocalDate(dateOnly);
                            appointmentDates.add(CalendarDay.from(localDate));

                            // 2. Logic for Finding Latest Upcoming Appointment
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

                            e.printStackTrace();
                        }
                    }

                    // A. Apply Decorators to Calendar
                    int dotColor = ContextCompat.getColor(CalendarActivity.this, R.color.red_primary);
                    calendarView.addDecorator(new EventDecorator(dotColor, appointmentDates));

                    // B. Display and Highlight the Latest Upcoming Appointment
                    if (upcomingAppointmentDoc != null) {
                        // Display in the Upcoming Card
                        displayAppointment(upcomingAppointmentDoc);

                        // Highlight the date on the calendar
                        String latestDateStr = upcomingAppointmentDoc.getString("date");
                        if (latestDateStr != null) {
                            try {
                                Date latestDate = dateFormat.parse(latestDateStr);
                                org.threeten.bp.LocalDate localDate = convertToLocalDate(latestDate);
                                CalendarDay latestCalendarDay = CalendarDay.from(localDate);

                                // Select the date and scroll the calendar view to it
                                calendarView.setSelectedDate(latestCalendarDay);
                                calendarView.setCurrentDate(latestCalendarDay);

                            } catch (Exception e) {
                                // Date parsing failed
                                e.printStackTrace();
                            }
                        }
                    } else {
                        displayNoAppointment();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load appointments.", Toast.LENGTH_SHORT).show();
                    displayNoAppointment();
                });
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
            // Use a 5-pixel dot span to mark the date
            view.addSpan(new DotSpan(5, color));
        }
    }

    private void fetchLatestUpcomingAppointment() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Get today's date in milliseconds
        long now = new Date().getTime();

        db.collection("appointments")
                .whereEqualTo("userId", userId) // Filter by current user
                .whereIn("status", List.of("pending", "confirmed")) // Only show active appointments
                // Firestore can only order by one field after a range/inequality filter (which we will simulate)
                // We'll rely on the client-side filtering for time and status for accuracy.
                // For simplicity and to use Firestore indexes efficiently, we'll order by date.
                .orderBy("date", Query.Direction.ASCENDING)
                .limit(20) // Limit the initial fetch size
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault());
                    long smallestFutureDifference = Long.MAX_VALUE;
                    DocumentSnapshot upcomingAppointment = null;

                    // 2. Iterate and find the truly NEXT appointment (considering both date and time)
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String dateStr = doc.getString("date"); // Format: yyyy-MM-dd
                        String timeStr = doc.getString("time"); // Format: h:mm a (e.g., 9:00 am)

                        if (dateStr == null || timeStr == null) continue;

                        try {
                            Date appointmentDateTime = dateFormat.parse(dateStr + " " + timeStr);
                            long appointmentTimeMillis = appointmentDateTime.getTime();

                            // Check if the appointment is in the future
                            if (appointmentTimeMillis >= now) {
                                long timeDifference = appointmentTimeMillis - now;

                                // Check if this appointment is closer than the smallest found so far
                                if (timeDifference < smallestFutureDifference) {
                                    smallestFutureDifference = timeDifference;
                                    upcomingAppointment = doc;
                                }
                            }
                        } catch (Exception e) {
                            // Handle parsing error
                        }
                    }

                    // 3. Display the result
                    if (upcomingAppointment != null) {
                        displayAppointment(upcomingAppointment);
                    } else {
                        displayNoAppointment();
                    }

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load appointments.", Toast.LENGTH_SHORT).show();
                    displayNoAppointment();
                });
    }
    private void displayAppointment(DocumentSnapshot appointmentDoc) {
        String docName = appointmentDoc.getString("doctorName");
        String dateStr = appointmentDoc.getString("date");
        String timeStr = appointmentDoc.getString("time");

        // Optional: Format the date for better display
        String formattedDate = dateStr;
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
            formattedDate = displayFormat.format(dbFormat.parse(dateStr));
        } catch (Exception e) {
            // Use raw date if parsing fails
        }

        doctorName.setText(docName != null ? docName : "N/A");
        appointmentDate.setText(formattedDate);
        appointmentTime.setText(timeStr != null ? timeStr : "N/A");
    }

    private void displayNoAppointment() {
        doctorName.setText("No Upcoming Appointments");
        appointmentDate.setText("Check back later!");
        appointmentTime.setText("");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}