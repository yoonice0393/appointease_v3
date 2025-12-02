        package com.example.sttherese.patient.activities;

        import android.content.Intent;
        import android.content.SharedPreferences;
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
        import androidx.recyclerview.widget.LinearLayoutManager;
        import androidx.recyclerview.widget.RecyclerView;

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

            // Header Views
            private TextView tvGreeting, tvUserName;
            private ImageView ivNotification, ivProfile, btnSearch;
            private EditText etSearch;

            // RecyclerViews
            private RecyclerView rvUpcomingAppointments, rvDoctors;
            private AppointmentAdapter appointmentAdapter;
            private DoctorAdapter doctorAdapter;

            // Empty State Views
            private LinearLayout layoutAppointments;
            private CardView layoutEmptyState;

            private MaterialButton btnBookAppointment;
            private ProgressBar progressBar;

            // Filter Chips
            private ChipGroup chipGroup;
            private Chip chipAll, chipObGyne, chipMedical;
//            private TextView tvViewAll;

            // Bottom Navigation
            private LinearLayout btnHome, btnDoctor, btnCalendar, btnHistory;
            private ImageView btnAdd;

            // Firestore
            private FirebaseFirestore db;

            private String userDocId;
            // Recent Visit Views
            private TextView tvVisitDate, tvVisitDay, tvServiceType, tvViewAll;
            // You need a layout for the recent visit card to show/hide it
            private CardView recentVisitCard;
            private CardView emptyRecentVisitCard;

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_home2);

                db = FirebaseFirestore.getInstance();

                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                userDocId = prefs.getString("user_doc_id", null);


                if (userDocId == null) {
                    Toast.makeText(this, "Authentication required. Please login.", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(Home.this, SignInPage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    finish();
                }
                // ========================================================================

                initializeViews();
                setupRecyclerViews();
                setupGreeting();
                setupClickListeners();
                fetchUserName();
                fetchAppointments();
                fetchDoctors("All");
                fetchRecentVisit();


                btnBookAppointment.setOnClickListener(v -> {
                    Intent intent = new Intent(Home.this, BookingAppointmentActivity.class);
                    startActivity(intent);
                });

                btnAdd.setOnClickListener(v -> {
                    Intent intent = new Intent(Home.this, BookingAppointmentActivity.class);
                    startActivity(intent);
                });

//                tvViewAllVisits.setOnClickListener(v -> {
//                    Intent intent = new Intent(Home.this, HistoryActivity.class);
//                    startActivity(intent);
//                });


            }

            private void fetchRecentVisit() {
                // 1. Initial visibility: Assume no recent visit until confirmed
                recentVisitCard.setVisibility(View.GONE);
                emptyRecentVisitCard.setVisibility(View.VISIBLE);

                // We specifically look for COMPLETED appointments sorted by date descending (latest first).
                db.collection("appointments")
                        .whereEqualTo("userId", userDocId)
                        .whereEqualTo("status", "completed") // NOTE: Status must match your Firestore value EXACTLY! (lowercase)
                        .orderBy("date", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshots -> {

                            if (querySnapshots != null && !querySnapshots.isEmpty()) {
                                DocumentSnapshot doc = querySnapshots.getDocuments().get(0);

                                // --- Data Extraction from Booking Document ---
                                String doctorId = doc.getString("doctorId");
                                String dateString = doc.getString("date");
                                // The user sees the appointmentType as the service description.
                                String serviceType = doc.getString("specialty");

                                // Check for critical data before proceeding
                                if (doctorId == null || dateString == null || serviceType == null) {
                                    Log.w(TAG, "Recent visit data incomplete, hiding card.");
                                    recentVisitCard.setVisibility(View.GONE);
                                    emptyRecentVisitCard.setVisibility(View.VISIBLE);
                                    return;
                                }

                                // --- 1. Date Formatting ---
                                try {
                                    SimpleDateFormat firebaseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(firebaseFormat.parse(dateString));

                                    SimpleDateFormat displayDate = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                                    SimpleDateFormat displayDay = new SimpleDateFormat("EEEE", Locale.getDefault());

                                    tvVisitDate.setText(displayDate.format(cal.getTime()));
                                    tvVisitDay.setText(displayDay.format(cal.getTime()));

                                    // Set the service type immediately
                                    tvServiceType.setText(serviceType);

                                } catch (Exception e) {
                                    Log.e(TAG, "Date parsing error for recent visit", e);
                                    tvVisitDate.setText(dateString);
                                    tvVisitDay.setText("");
                                }

                                // --- 2. Final UI Update (Show Data Card) ---
                                // Only show the card if we successfully processed the data
                                recentVisitCard.setVisibility(View.VISIBLE);
                                emptyRecentVisitCard.setVisibility(View.GONE);


                            } else {
                                // Case: No completed bookings found
                                recentVisitCard.setVisibility(View.GONE);
                                emptyRecentVisitCard.setVisibility(View.VISIBLE);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to load recent visit", e);
                            // On failure, display the empty state
                            recentVisitCard.setVisibility(View.GONE);
                            emptyRecentVisitCard.setVisibility(View.VISIBLE);
                        });
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
//                tvViewAllVisits = findViewById(R.id.tvViewAllVisits);
                recentVisitCard = findViewById(R.id.recentVisitCard); // Assuming you add this ID to the XML
                emptyRecentVisitCard = findViewById(R.id.emptyRecentVisitCard);

                if (chipAll != null) chipAll.setChecked(true);
            }

                private void setupRecyclerViews() {
                    // --- Appointments ---
                    // 1. Get today's date in a comparable format (e.g., "yyyy-MM-dd")
                    //    We use the system time to ensure we only get future appointments.
                    String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

                    // 2. Build the new Query
                    // Use "bookings" collection name
                    Query appointmentQuery = db.collection("appointments")
                            .whereEqualTo("userId", userDocId)
                            .whereEqualTo("status", "pending")
                            .whereGreaterThanOrEqualTo("date", todayDate)
                            .orderBy("date", Query.Direction.ASCENDING)
                            .limit(1);;

                    // CRITICAL: This query now requires a Firestore Index!
                    // (Collection: 'bookings', Fields: 'userId' ASC, 'status' ASC, 'date' ASC)


                    AppointmentAdapter.OnAppointmentClickListener appointmentClickListener = appointment -> {
                        // Navigate to CalendarActivity when an upcoming card is clicked
                        Intent intent = new Intent(Home.this, CalendarActivity.class);

                        // Optional: Pass the appointment ID so CalendarActivity can highlight it
    //                    intent.putExtra("APPOINTMENT_ID", appointment.getId());

                        startActivity(intent);
                    };

                    appointmentAdapter = new AppointmentAdapter(
                            this,
                            appointmentClickListener,
                            appointmentQuery,
                            itemCount -> { // <-- New Listener Implementation

                        // Logic to toggle visibility of empty state and appointments list
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        if (itemCount > 0) {
                            layoutAppointments.setVisibility(View.VISIBLE);
                            layoutEmptyState.setVisibility(View.GONE);
                        } else {
                            layoutAppointments.setVisibility(View.GONE);
                            layoutEmptyState.setVisibility(View.VISIBLE);
                        }
                    },
                            "patient");

                    rvUpcomingAppointments.setLayoutManager(new LinearLayoutManager(this));
                    rvUpcomingAppointments.setAdapter(appointmentAdapter);

                    // --- Doctors ---
                    Query doctorQuery = db.collection("doctors");
                    doctorAdapter = new DoctorAdapter(this, doctor -> {
                        Toast.makeText(Home.this, "Booking with " + doctor.getName(), Toast.LENGTH_SHORT).show();
                        // TODO: Start booking activity
                    }, doctorQuery);

                    rvDoctors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                    rvDoctors.setAdapter(doctorAdapter);
                }

            private void setupGreeting() {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                String greeting = (hour < 12) ? "GOOD MORNING" : (hour < 17) ? "GOOD AFTERNOON" : "GOOD EVENING";
                tvGreeting.setText(greeting);
            }

            private void setupClickListeners() {


                ivNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
                ivProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

                tvViewAll.setOnClickListener(v -> startActivity(new Intent(this, DoctorsActivity.class)));

                chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                    if (!checkedIds.isEmpty()) {
                        int selectedId = checkedIds.get(0);
                        if (selectedId == R.id.chipAll) fetchDoctors("All");
                        else if (selectedId == R.id.chipObGyne) fetchDoctors("OB-GYNE");
                        else if (selectedId == R.id.chipMedical) fetchDoctors("INTERNAL MEDICINE");
                    }
                });

                btnHome.setOnClickListener(v -> Toast.makeText(this, "Already at Home", Toast.LENGTH_SHORT).show());
                btnDoctor.setOnClickListener(v -> startActivity(new Intent(this, DoctorsActivity.class)));
                btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
                btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));

                if (btnBookAppointment != null) {
                    // This button is in the "empty state" card
                    btnBookAppointment.setOnClickListener(v -> Toast.makeText(this, "Book Appointment", Toast.LENGTH_SHORT).show());
                }
            }
            private void showScheduleDialog(String doctorId, String doctorName) {
                android.app.Dialog dialog = new android.app.Dialog(this);
                dialog.setContentView(R.layout.dialog_doctor_schedule);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.setCancelable(true);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }

                RecyclerView recyclerView = dialog.findViewById(R.id.scheduleRecyclerView);
                LinearLayout emptyStateView = dialog.findViewById(R.id.emptyStateView);

                TextView tvHeader = dialog.findViewById(R.id.tvDoctorNameHeader);
                TextView tvDoctorSpecialtyHeader = dialog.findViewById(R.id.tvDoctorSpecialtyHeader);
                ImageView closeBtnX = dialog.findViewById(R.id.closeButton);

                tvHeader.setText(doctorName);

                // Fetch doctor's specialty from Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("doctors")
                        .document(doctorId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String specialty = documentSnapshot.getString("specialty");
                                if (specialty != null && !specialty.isEmpty()) {
                                    tvDoctorSpecialtyHeader.setText(specialty);
                                } else {
                                    tvDoctorSpecialtyHeader.setText("General Practitioner");
                                }
                            } else {
                                tvDoctorSpecialtyHeader.setText("General Practitioner");
                                Log.w("ScheduleDialog", "Doctor document not found for ID: " + doctorId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ScheduleDialog", "Error fetching doctor specialty: ", e);
                            tvDoctorSpecialtyHeader.setText("General Practitioner");
                        });

                closeBtnX.setOnClickListener(v -> dialog.dismiss());

                List<ScheduleSlot> scheduleList = new ArrayList<>();
                ScheduleSlotAdapter adapter = new ScheduleSlotAdapter(this, scheduleList, slot -> {
                    Toast.makeText(this, "Proceeding to book a slot on " + slot.getDate() +
                            " between " + slot.getStartTime() + " and " + slot.getEndTime(), Toast.LENGTH_LONG).show();
                    // TODO: Start your booking confirmation activity/fragment here, passing date and doctorId
                    dialog.dismiss();
                });

                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);

                loadScheduleData(doctorId, scheduleList, adapter, emptyStateView, recyclerView);

                dialog.show();
            }
            private void loadScheduleData(String doctorId, List<ScheduleSlot> scheduleList,
                                          ScheduleSlotAdapter adapter, LinearLayout emptyStateView,
                                          RecyclerView recyclerView) {

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("clinic_schedules")
                        .whereEqualTo("doctor_id", doctorId)
                        .get()
                        .addOnSuccessListener(fixedSchedulesSnapshot -> {

                            Map<String, DocumentSnapshot> fixedSchedules = new HashMap<>();
                            Log.d("ScheduleDebug", "Fixed Schedules loaded: " + fixedSchedulesSnapshot.size());

                            for (DocumentSnapshot doc : fixedSchedulesSnapshot.getDocuments()) {
                                String dayOfWeek = doc.getString("day_of_week");
                                if (dayOfWeek != null) {
                                    fixedSchedules.put(dayOfWeek.toLowerCase(Locale.ROOT), doc);
                                }
                            }

                            // Step 2: Fetch schedule exceptions and existing appointments
                            // Note: 'doctor_id' is correct for exceptions
                            db.collection("schedule_exceptions")
                                    .whereEqualTo("doctor_id", doctorId)
                                    .whereGreaterThanOrEqualTo("date", getCurrentDateString())
                                    .get()
                                    .addOnSuccessListener(exceptionsSnapshot -> {

                                        // Note: 'doctorId' is correct for appointments
                                        db.collection("appointments")
                                                .whereEqualTo("doctorId", doctorId)
                                                .whereGreaterThanOrEqualTo("date", getCurrentDateString())
                                                .whereIn("status", Arrays.asList("pending", "confirmed"))
                                                .get()
                                                .addOnSuccessListener(appointmentsSnapshot -> {

                                                    // Only needed if you wanted to mark a day as partially booked.
                                                    // For this daily view, we won't use bookedSlots to filter out individual time slots.

                                                    // Generate schedule for the next 14 days
                                                    generateDailySlots(fixedSchedules, exceptionsSnapshot.getDocuments(), scheduleList);

                                                    Log.d("ScheduleDebug", "Generated daily entries: " + scheduleList.size());

                                                    adapter.notifyDataSetChanged();

                                                    // 5. Display results
                                                    if (scheduleList.isEmpty()) {
                                                        emptyStateView.setVisibility(View.VISIBLE);
                                                        recyclerView.setVisibility(View.GONE);
                                                    } else {
                                                        emptyStateView.setVisibility(View.GONE);
                                                        recyclerView.setVisibility(View.VISIBLE);
                                                    }
                                                });
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ScheduleDialog", "Error loading schedule data: ", e);
                            Toast.makeText(Home.this, "Failed to load schedule.", Toast.LENGTH_SHORT).show();
                            emptyStateView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        });
            }

            private void generateDailySlots(Map<String, DocumentSnapshot> fixedSchedules,
                                            List<DocumentSnapshot> exceptions,
                                            List<ScheduleSlot> resultList) {

                resultList.clear();

                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH); // Full day name (Monday)
                SimpleDateFormat shortDayFormat = new SimpleDateFormat("EEE", Locale.ENGLISH); // MON, TUE
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

                // Date format for display (e.g., Nov 17, 2025)
                SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                // Map exceptions for quick lookup: date -> DocumentSnapshot
                Map<String, DocumentSnapshot> exceptionMap = new HashMap<>();
                for (DocumentSnapshot doc : exceptions) {
                    String date = doc.getString("date");
                    if (date != null) exceptionMap.put(date, doc);
                }

                // Iterate over the next 14 days
                for (int i = 0; i < 14; i++) {
                    String dateString = dateFormat.format(calendar.getTime());
                    String displayDate = displayDateFormat.format(calendar.getTime());
                    String dayNameFull = dayFormat.format(calendar.getTime());
                    String dayNameShort = shortDayFormat.format(calendar.getTime());
                    String dayOfWeek = dayNameFull.toLowerCase(Locale.ROOT);

                    // 1. Check for All-Day Exception
                    if (exceptionMap.containsKey(dateString)) {
                        DocumentSnapshot exceptionDoc = exceptionMap.get(dateString);
                        if (Boolean.TRUE.equals(exceptionDoc.getBoolean("is_all_day"))) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1);
                            continue; // Skip this date
                        }
                    }

                    // 2. Check Fixed Schedule
                    if (fixedSchedules.containsKey(dayOfWeek)) {
                        DocumentSnapshot scheduleDoc = fixedSchedules.get(dayOfWeek);

                        String startTimeStr = scheduleDoc.getString("start_time");
                        String endTimeStr = scheduleDoc.getString("end_time");

                        if (startTimeStr == null || endTimeStr == null) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1);
                            continue;
                        }

                        // Convert times to readable 12-hour format
                        String displayStartTime = formatTime(startTimeStr);
                        String displayEndTime = formatTime(endTimeStr);

                        // 3. Apply Partial-Day Exception Logic (simplified: we ignore it for a full-day view)
                        // If you need to show "9:00 AM - 12:00 PM, then 2:00 PM - 5:00 PM", you must create TWO ScheduleSlot objects.

                        // 4. Create single Daily Schedule Slot
                        ScheduleSlot dailySlot = new ScheduleSlot();
                        dailySlot.setDate(displayDate);
                        dailySlot.setDay(dayNameFull);
                        dailySlot.setDayShort(dayNameShort.toUpperCase(Locale.ROOT));
                        dailySlot.setStartTime(displayStartTime);
                        dailySlot.setEndTime(displayEndTime);
                        dailySlot.setStatus("Available"); // Status is always available for the day

                        resultList.add(dailySlot);
                    }

                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
            }

            private String formatTime(String timeStr) {
                if (timeStr == null) return "N/A";
                try {
                    SimpleDateFormat dbFormat = new SimpleDateFormat("H:mm", Locale.ROOT);
                    SimpleDateFormat displayFormat = new SimpleDateFormat("h:mm a", Locale.ROOT);
                    Date date = dbFormat.parse(timeStr);
                    return displayFormat.format(date);
                } catch (Exception e) {
                    Log.e("ScheduleHelper", "Failed to parse time string: " + timeStr, e);
                    return timeStr;
                }
            }

            private String getCurrentDateString() {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
            }

            private void fetchUserName() {
                // userDocId is the Firebase Auth UID (e.g., xvNf...60e2)

                db.collection("patients")
                        .whereEqualTo("userId", userDocId) // Query by the field 'userId'
                        .limit(1)
                        .addSnapshotListener((querySnapshots, e) -> {
                            if (e != null) {
                                Log.w(TAG, "Listen failed for patients query.", e);
                                tvUserName.setText("User!");
                                return;
                            }

                            if (querySnapshots != null && !querySnapshots.isEmpty()) {
                                // Get the first matching document (there should only be one)
                                com.google.firebase.firestore.DocumentSnapshot snapshot = querySnapshots.getDocuments().get(0);

                                // Note: The field name in your screenshot is 'first_name', not 'firstName'
                                String firstName = capitalizeWords(snapshot.getString("first_name"));
                                // Assuming 'tvUserName' is your TextView
                                tvUserName.setText(firstName != null ? firstName + "!" : "User!");
                            } else {
                                tvUserName.setText("User!");
                            }
                        });
            }
            private String capitalizeWords(String text) {
                if (text == null || text.isEmpty()) return text;
                String[] words = text.trim().split("\\s+");
                StringBuilder result = new StringBuilder();
                for (String word : words) {
                    if (word.length() > 0)
                        result.append(Character.toUpperCase(word.charAt(0)))
                                .append(word.substring(1).toLowerCase())
                                .append(" ");
                }
                return result.toString().trim();
            }
            private void fetchAppointments() {
                // PROGRESS BAR FIX: We only SHOW the bar here.
                // The adapter is now responsible for HIDING it.
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                // The adapter's snapshot listener will handle the rest.
            }

            private void fetchDoctors(String specialty) {
                Query query = db.collection("doctors");

                if (!specialty.equals("All")) {
                    query = query.whereEqualTo("specialty", specialty);
                }
                doctorAdapter.removeListener(); // Remove previous listener
                doctorAdapter = new DoctorAdapter(this, doctor -> {
                    showScheduleDialog(doctor.getId(), doctor.getName());
                }, query);
                rvDoctors.setAdapter(doctorAdapter);
            }

            private void performSearch(String query) {
                // ⚠️ This query requires a Firestore Index!
                // (Collection: 'doctors', Field: 'name' ASC)
                Query searchQuery = db.collection("doctors")
                        .orderBy("name")
                        .startAt(query)
                        .endAt(query + "\uf8ff");

                doctorAdapter.removeListener();
                doctorAdapter = new DoctorAdapter(this, doctor -> {
                    Toast.makeText(Home.this, "Booking with " + doctor.getName(), Toast.LENGTH_SHORT).show();
                }, searchQuery);
                rvDoctors.setAdapter(doctorAdapter);
            }

            @Override
            protected void onResume() {
                super.onResume();
                setupGreeting();
                // You might want to re-attach listeners if you detach them in onPause
            }

            @Override
            protected void onDestroy() {
                super.onDestroy();
                // Clean up listeners to prevent memory leaks
                if (appointmentAdapter != null) appointmentAdapter.removeListener();
                if (doctorAdapter != null) doctorAdapter.removeListener();
            }
        }