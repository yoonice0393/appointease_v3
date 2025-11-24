package com.example.sttherese.patient.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.adapters.DoctorAdapter;
import com.example.sttherese.adapters.ScheduleSlotAdapter;
import com.example.sttherese.models.ScheduleSlot; // Using the updated model
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DoctorsActivity extends AppCompatActivity {

    private RecyclerView rvDoctors;
    private DoctorAdapter doctorAdapter;

    private EditText etSearch;
    private ImageView btnSearch;
    private ChipGroup chipGroup;
    private Chip chipAll;

    // Bottom Navigation
    private LinearLayout btnHome, btnDoctor, btnCalendar, btnHistory;
    private ImageView btnAdd;

    private FirebaseFirestore db;

    private String selectedFilter = "All";
    private Map<String, Chip> specialtyChips = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctors);

        db = FirebaseFirestore.getInstance();
        initializeViews();
        loadSpecialtyChips();
        setupFilters();
        setupSearch();
        setupRecyclerView();
        setupClickListeners();

        applyQuery("");
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyQuery(s.toString().trim());
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
         btnHome.setOnClickListener(v -> startActivity(new Intent(this, Home.class)));
         btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
         btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        btnDoctor.setOnClickListener(v -> Toast.makeText(this, "Already on Doctors", Toast.LENGTH_SHORT).show());
        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, BookingAppointmentActivity.class)));
    }

    private void initializeViews() {
        rvDoctors = findViewById(R.id.rvDoctors);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);

        chipGroup = findViewById(R.id.chipGroup);
        chipAll = findViewById(R.id.chipAll);

        btnHome = findViewById(R.id.btnHome);
        btnDoctor = findViewById(R.id.btnDoctor);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnHistory = findViewById(R.id.btnHistory);
        btnAdd = findViewById(R.id.btnAdd);

        rvDoctors.setLayoutManager(new LinearLayoutManager(this));
        chipAll.setChecked(true);
    }

    private void setupRecyclerView() {
        // Initial adapter setup is done in applyQuery
    }

    private void loadSpecialtyChips() {
        // Step 1: Clear old content and re-add the static chipAll
        chipGroup.removeAllViews();
        // The chipAll instance MUST be retained from initializeViews()
        if (chipAll != null) {
            chipGroup.addView(chipAll);
            chipAll.setChecked(true); // Ensure 'All' is selected by default on load
            selectedFilter = "All";  // Ensure the filter variable reflects the default
        }

        db.collection("doctors")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> specialties = new HashSet<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String specialty = document.getString("specialty");
                        if (specialty != null && !specialty.isEmpty()) {
                            specialties.add(specialty);
                        }
                    }

                    // Create a chip for each unique specialty
                    for (String specialty : specialties) {
                        Chip chip = createSpecialtyChip(specialty);
                        chipGroup.addView(chip);
                        specialtyChips.put(specialty, chip);
                    }

                    // Re-setup filters after chips are loaded, which will attach the listener
                    setupFilters();
                    applyQuery("");
                })
                .addOnFailureListener(e -> {
                    Log.e("DoctorsActivity", "Error loading specialties: " + e.getMessage());
                    Toast.makeText(this, "Failed to load specialty filters.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Creates and configures a single specialty Chip.
     */
    private Chip createSpecialtyChip(String specialty) {
        // Inflate the chip from the dedicated layout resource
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_specialty_item, chipGroup, false);

        chip.setText(specialty);
        chip.setTag(specialty);

        // Assign a unique ID (Still needed for the setOnCheckedStateChangeListener)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            chip.setId(View.generateViewId());
        } else {
            chip.setId(specialty.hashCode());
        }

        return chip;
    }

    private void setupFilters() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int selectedId = checkedIds.get(0);

                Chip selectedChip = findViewById(selectedId);
                if (selectedChip != null) {

                    // The core logic to check for the 'All' chip's ID
                    if (selectedChip.getId() == R.id.chipAll) {
                        selectedFilter = "All"; // ✅ This is the correct filter value
                    } else {
                        selectedFilter = (String) selectedChip.getTag();
                    }
                }
                updateListOnFilter();
            } else {
                // Safety measure: re-select "All"
                chipAll.setChecked(true);
                selectedFilter = "All";
                updateListOnFilter();
            }
        });
    }

    private void setupSearch() {
        btnSearch.setOnClickListener(v -> {
            String queryText = etSearch.getText().toString().trim();
            applyQuery(queryText);
        });
    }

    private void updateListOnFilter() {
        String currentQuery = etSearch.getText().toString().trim();
        applyQuery(currentQuery);
    }

    private void applyQuery(String searchQuery) {
        Query query = db.collection("doctors");

        if (!selectedFilter.equals("All")) {
            query = query.whereEqualTo("specialty", selectedFilter);
        }

        if (searchQuery != null && !searchQuery.isEmpty()) {
            query = query.orderBy("name")
                    .startAt(searchQuery)
                    .endAt(searchQuery + "\uf8ff");
        } else {
            query = query.orderBy("name");
        }

        if (doctorAdapter != null) doctorAdapter.removeListener();

        // Assuming Doctor model has getId() and getName()
        doctorAdapter = new DoctorAdapter(this, doctor -> {
            showScheduleDialog(doctor.getId(), doctor.getName());
        }, query);

        rvDoctors.setAdapter(doctorAdapter);
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

    /**
     * Executes the complex logic of fetching, generating, and filtering schedule slots.
     */
    private void loadScheduleData(String doctorId, List<ScheduleSlot> scheduleList,
                                  ScheduleSlotAdapter adapter, LinearLayout emptyStateView,
                                  RecyclerView recyclerView) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch fixed weekly schedules for the doctor
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

                    // Fetch schedule exceptions and existing appointments

                    db.collection("schedule_exceptions")
                            .whereEqualTo("doctor_id", doctorId)
                            .whereGreaterThanOrEqualTo("date", getCurrentDateString())
                            .get()
                            .addOnSuccessListener(exceptionsSnapshot -> {


                                db.collection("appointments")
                                        .whereEqualTo("doctorId", doctorId)
                                        .whereGreaterThanOrEqualTo("date", getCurrentDateString())
                                        .whereIn("status", Arrays.asList("pending", "confirmed"))
                                        .get()
                                        .addOnSuccessListener(appointmentsSnapshot -> {



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
                    Toast.makeText(DoctorsActivity.this, "Failed to load schedule.", Toast.LENGTH_SHORT).show();
                    emptyStateView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
    }

    /**
     * Generates a single schedule entry (start time - end time) for the next 14 working days.
     */
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


    /** Converts "9:00" or "17:00" string to "9:00 AM" or "5:00 PM". */
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (doctorAdapter != null) doctorAdapter.removeListener();
    }
}