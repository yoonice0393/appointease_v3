package com.example.sttherese;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

//import com.android.volley.Request;
//import com.android.volley.toolbox.StringRequest;
//import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomePage extends AppCompatActivity {

        // Header Views
        private TextView tvGreeting, tvUserName;
        private ImageView ivNotification, ivProfile, btnSearch;
        private EditText etSearch;

        // Appointment Card Views
        private CardView appointmentCard;
        private TextView tvDoctorName, tvDoctorSpecialty, tvAppointmentDate, tvAppointmentTime;
        private ImageView btnNavigate;

        // Recent Visit Views
        private TextView tvVisitDate, tvVisitDay, tvServiceType;

        // Doctor Card Views
        private TextView tvDoctorNameCard, tvDoctorSpecialtyCard, tvDoctorHours;
        private MaterialButton btnBookNow;
        private ImageView ivDoctorImage;

        // Filter Chips
        private ChipGroup chipGroup;
        private Chip chipAll, chipObGyn, chipPerinatologist;
        private TextView tvViewAll;

        // Bottom Navigation
        private LinearLayout btnHome, btnLocation, btnCalendar, btnMenu;
        private ImageView btnAdd;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_home_with_nav);

            initializeViews();
            setupGreeting();
            setupClickListeners();
            loadDefaultData();
        }

        private void initializeViews() {
            // Header
            tvGreeting = findViewById(R.id.tvGreeting);
            tvUserName = findViewById(R.id.tvUserName);
            ivNotification = findViewById(R.id.ivNotification);
            ivProfile = findViewById(R.id.ivProfile);
            etSearch = findViewById(R.id.etSearch);
            btnSearch = findViewById(R.id.btnSearch);

            // Upcoming Appointment
            appointmentCard = findViewById(R.id.appointmentCard);
            tvDoctorName = findViewById(R.id.tvDoctorName);
            tvDoctorSpecialty = findViewById(R.id.tvDoctorSpecialty);
            tvAppointmentDate = findViewById(R.id.tvAppointmentDate);
            tvAppointmentTime = findViewById(R.id.tvAppointmentTime);
            btnNavigate = findViewById(R.id.btnNavigate);

            // Recent Visit
            tvVisitDate = findViewById(R.id.tvVisitDate);
            tvVisitDay = findViewById(R.id.tvVisitDay);
            tvServiceType = findViewById(R.id.tvServiceType);

            // Doctor Card
            tvDoctorNameCard = findViewById(R.id.tvDoctorNameCard);
            tvDoctorSpecialtyCard = findViewById(R.id.tvDoctorSpecialtyCard);
            tvDoctorHours = findViewById(R.id.tvDoctorHours);
            btnBookNow = findViewById(R.id.btnBookNow);
            ivDoctorImage = findViewById(R.id.ivDoctorImage);

            // Filters
            chipGroup = findViewById(R.id.chipGroup);
            chipAll = findViewById(R.id.chipAll);
            chipObGyn = findViewById(R.id.chipObGyn);
            chipPerinatologist = findViewById(R.id.chipPerinatologist);
            tvViewAll = findViewById(R.id.tvViewAll);

            // Bottom Navigation
            btnHome = findViewById(R.id.btnHome);
            btnLocation = findViewById(R.id.btnLocation);
            btnCalendar = findViewById(R.id.btnCalendar);
            btnMenu = findViewById(R.id.btnMenu);
            btnAdd = findViewById(R.id.btnAdd);
        }

        private void setupGreeting() {
            Calendar calendar = Calendar.getInstance();
            int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

            String greeting;
            if (hourOfDay >= 0 && hourOfDay < 12) {
                greeting = "GOOD MORNING";
            } else if (hourOfDay >= 12 && hourOfDay < 17) {
                greeting = "GOOD AFTERNOON";
            } else {
                greeting = "GOOD EVENING";
            }

            tvGreeting.setText(greeting);
            fetchUserNameFromDatabase();
        }
    private void fetchUserNameFromDatabase() {
        String url = "http://192.168.88.250/user_api/get_user.php";

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        if (userId == -1) {
            tvUserName.setText("User!");
            return;
        }

//        StringRequest request = new StringRequest(Request.Method.POST, url,
//                response -> {
//                    try {
//                        JSONObject json = new JSONObject(response);
//                        if (json.getBoolean("success")) {
//                            String firstName = json.getString("first_name");
//                            tvUserName.setText(firstName + "!");
//                        } else {
//                            tvUserName.setText("User!");
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        tvUserName.setText("User!");
//                    }
//                },
//                error -> {
//                    error.printStackTrace();
//                    tvUserName.setText("User!");
//                }) {
//            @Override
//            protected Map<String, String> getParams() {
//                Map<String, String> params = new HashMap<>();
//                params.put("user_id", String.valueOf(userId));
//                return params;
//            }
//        };
//        Volley.newRequestQueue(this).add(request);
    }

        private void setupClickListeners() {
            // Search functionality
            btnSearch.setOnClickListener(v -> {
                String query = etSearch.getText().toString();
                if (!query.isEmpty()) {
                    performSearch(query);
                } else {
                    showToast("Please enter a search term");
                }
            });

            // Real-time search
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() > 0) {
                        // Perform search as user types
                        performSearch(s.toString());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Notification
            ivNotification.setOnClickListener(v -> {
                showToast("Notifications");
                // Navigate to NotificationsActivity
                // startActivity(new Intent(HomePage.this, NotificationsActivity.class));
            });

            // Profile
            ivProfile.setOnClickListener(v -> {
                showToast("Profile");
                // Navigate to ProfileActivity
                // startActivity(new Intent(HomePage.this, ProfileActivity.class));
            });

            // Appointment Card Navigation
            btnNavigate.setOnClickListener(v -> {
                showToast("View Appointment Details");
                // Navigate to AppointmentDetailsActivity
                // Intent intent = new Intent(HomePage.this, AppointmentDetailsActivity.class);
                // intent.putExtra("doctor_name", tvDoctorName.getText().toString());
                // startActivity(intent);
            });

            appointmentCard.setOnClickListener(v -> {
                showToast("View Full Appointment");
                // Same as btnNavigate
            });

            // Book Now Button
            btnBookNow.setOnClickListener(v -> {
                String doctorName = tvDoctorNameCard.getText().toString();
                showToast("Booking Appointment with " + doctorName);
                // Navigate to BookingActivity
                // Intent intent = new Intent(HomePage.this, BookingActivity.class);
                // intent.putExtra("doctor_name", doctorName);
                // startActivity(intent);
            });

            // View All Doctors
            tvViewAll.setOnClickListener(v -> {
                showToast("View All Doctors");
                // Navigate to DoctorsActivity with full list
                 startActivity(new Intent(HomePage.this, DoctorsActivity.class));
            });

            // Chip Group Selection
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty()) {
                    int selectedId = checkedIds.get(0);
                    if (selectedId == R.id.chipAll) {
                        filterDoctors("All");
                    } else if (selectedId == R.id.chipObGyn) {
                        filterDoctors("OB-GYN");
                    } else if (selectedId == R.id.chipPerinatologist) {
                        filterDoctors("Perinatologist");
                    }
                }
            });

            // Bottom Navigation
            btnHome.setOnClickListener(v -> {
                showToast("Already on Home");
                // Already on home page
            });

            btnLocation.setOnClickListener(v -> {
                showToast("Doctors");
                // Navigate to DoctorsActivity
                 startActivity(new Intent(HomePage.this, DoctorsActivity.class));
            });

            btnAdd.setOnClickListener(v -> {
                showToast("Add New Appointment");
                // Navigate to AddAppointmentActivity
                // startActivity(new Intent(HomePage.this, AddAppointmentActivity.class));
            });

            btnCalendar.setOnClickListener(v -> {
                showToast("Calendar");
                // Navigate to CalendarActivity
                 startActivity(new Intent(HomePage.this, CalendarActivity.class));
            });

            btnMenu.setOnClickListener(v -> {
                showToast("Menu");
                // Show menu dialog or navigate to MenuActivity
                // showMenuDialog();
            });
        }

        private void loadDefaultData() {
            // Set default chip selection
            chipAll.setChecked(true);

            // Load default doctor data (in real app, this would come from database)
            loadDoctorCard("Dr. John Doe", "General Practitioner", "9:00 am - 2:00 pm");
        }

        private void loadDoctorCard(String name, String specialty, String hours) {
            tvDoctorNameCard.setText(name);
            tvDoctorSpecialtyCard.setText(specialty);
            tvDoctorHours.setText(hours);
        }

        private void performSearch(String query) {
            showToast("Searching for: " + query);
            // In a real app, you would:
            // 1. Query your database
            // 2. Filter doctors list
            // 3. Update UI with results

            // Example:
            // List<Doctor> results = doctorDatabase.searchDoctors(query);
            // if (results.isEmpty()) {
            //     showToast("No doctors found");
            // } else {
            //     updateDoctorCard(results.get(0));
            // }
        }

        private void filterDoctors(String specialty) {
            showToast("Filtering by: " + specialty);

            // Update the doctor card based on filter
            switch (specialty) {
                case "All":
                    loadDoctorCard("Dr. John Doe", "General Practitioner", "9:00 am - 2:00 pm");
                    break;
                case "OB-GYN":
                    loadDoctorCard("Dr. Sarah Johnson", "OB-GYN", "10:00 am - 5:00 pm");
                    break;
                case "Perinatologist":
                    loadDoctorCard("Dr. Michael Chen", "Perinatologist", "8:00 am - 3:00 pm");
                    break;
            }

            // In a real app, you would filter from your database:
            // List<Doctor> filtered = doctorDatabase.getDoctorsBySpecialty(specialty);
            // updateDoctorsList(filtered);
        }

        private void showToast(String message) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onResume() {
            super.onResume();
            // Update greeting when returning to activity
            setupGreeting();
        }
    }