        package com.example.sttherese.patient.activities;

        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
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
        import com.google.android.material.button.MaterialButton;
        import com.google.android.material.chip.Chip;
        import com.google.android.material.chip.ChipGroup;
        import com.google.firebase.firestore.DocumentSnapshot;
        import com.google.firebase.firestore.FirebaseFirestore;
        import com.google.firebase.firestore.Query;

        import java.text.SimpleDateFormat;
        import java.util.Calendar;
        import java.util.Locale;

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
            private Chip chipAll, chipObGyn, chipPerinatologist;
            private TextView tvViewAll;

            // Bottom Navigation
            private LinearLayout btnHome, btnDoctor, btnCalendar, btnHistory;
            private ImageView btnAdd;

            // Firestore
            private FirebaseFirestore db;

            private String userDocId;
            // Recent Visit Views
            private TextView tvVisitDate, tvVisitDay, tvServiceType, tvViewAllVisits;
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
                    Intent intent = new Intent(Home.this, AppointmentDetailsActivity.class);
                    startActivity(intent);
                });

                btnAdd.setOnClickListener(v -> {
                    Intent intent = new Intent(Home.this, AppointmentDetailsActivity.class);
                    startActivity(intent);
                });


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
                etSearch = findViewById(R.id.etSearch);
                btnSearch = findViewById(R.id.btnSearch);

                rvUpcomingAppointments = findViewById(R.id.rvUpcomingAppointments);
                rvDoctors = findViewById(R.id.rvDoctors);


                layoutAppointments = findViewById(R.id.layoutAppointments);
                layoutEmptyState = findViewById(R.id.layoutEmptyState);

                btnBookAppointment = findViewById(R.id.btnBookAppointment);
                progressBar = findViewById(R.id.progressBar);

                chipGroup = findViewById(R.id.chipGroup);
                chipAll = findViewById(R.id.chipAll);
                chipObGyn = findViewById(R.id.chipObGyn);
                chipPerinatologist = findViewById(R.id.chipPerinatologist);
                tvViewAll = findViewById(R.id.tvViewAll);

                btnHome = findViewById(R.id.btnHome);
                btnDoctor = findViewById(R.id.btnDoctor);
                btnCalendar = findViewById(R.id.btnCalendar);
                btnHistory = findViewById(R.id.btnHistory);
                btnAdd = findViewById(R.id.btnAdd);

                tvVisitDate = findViewById(R.id.tvVisitDate);
                tvVisitDay = findViewById(R.id.tvVisitDay);
                tvServiceType = findViewById(R.id.tvServiceType);
                tvViewAllVisits = findViewById(R.id.tvViewAllVisits);
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
                btnSearch.setOnClickListener(v -> {
                    String query = etSearch.getText().toString().trim();
                    if (!query.isEmpty()) performSearch(query);
                });

                ivNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
                ivProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

                tvViewAll.setOnClickListener(v -> startActivity(new Intent(this, DoctorsActivity.class)));

                chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                    if (!checkedIds.isEmpty()) {
                        int selectedId = checkedIds.get(0);
                        if (selectedId == R.id.chipAll) fetchDoctors("All");
                        else if (selectedId == R.id.chipObGyn) fetchDoctors("OB-GYN");
                        else if (selectedId == R.id.chipPerinatologist) fetchDoctors("Perinatologist");
                    }
                });

                btnHome.setOnClickListener(v -> Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show());
                btnDoctor.setOnClickListener(v -> startActivity(new Intent(this, DoctorsActivity.class)));
                btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
                btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
                btnAdd.setOnClickListener(v -> Toast.makeText(this, "Add New", Toast.LENGTH_SHORT).show());

                if (btnBookAppointment != null) {
                    // This button is in the "empty state" card
                    btnBookAppointment.setOnClickListener(v -> Toast.makeText(this, "Book Appointment", Toast.LENGTH_SHORT).show());
                }
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
                    Toast.makeText(Home.this, "Booking with " + doctor.getName(), Toast.LENGTH_SHORT).show();
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