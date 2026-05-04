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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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
    private FirebaseAuth mAuth; 
    private String selectedDoctorId = null;
    private String selectedDate = null;
    private String selectedTime = null;
    private String selectedAppointmentType = null;
    private Doctor selectedDoctor = null;

    private List<String> appointmentTypes = new ArrayList<>();
    private Button selectedTimeButton = null;

    private Query doctorsQuery = null;
    private DoctorAdapter doctorAdapter; 

    private String selectedCategorySchedule = null;
    private List<String> availableDaysForCategory = new ArrayList<>(); 
    private String selectedSpecialty = null; 
    private List<Map<String, Object>> servicesListWithSpecialty = new ArrayList<>(); 


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_appointment);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance(); 
        warmUpRenderServer();
        initializeViews();
        setupListeners();
        fetchAppointmentTypes();
    }

    private void warmUpRenderServer() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://sttherese-api.onrender.com/ping.php");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.getResponseCode();
                conn.disconnect();
                android.util.Log.d("RENDER_PING", "Server warmed up");
            } catch (Exception e) {
                android.util.Log.e("RENDER_PING", "Warm up failed: " + e.getMessage());
            }
        }).start();
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
        closeButton.setOnClickListener(v -> finish());

        spinnerAppointmentType.setOnItemClickListener((parent, view, position, id) -> {
            selectedAppointmentType = appointmentTypes.get(position);
            fetchDoctorsByType(selectedAppointmentType);
        });

        doctorCard.setOnClickListener(v -> {
            if (selectedAppointmentType != null) {
                showDoctorSelectionDialog();
            } else {
                Toast.makeText(this, "Please select appointment type first", Toast.LENGTH_SHORT).show();
            }
        });

        buttonPickDate.setOnClickListener(v -> showDatePicker());
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

                    int totalSpecialties = specialtySnapshots.size();
                    final int[] processedCount = {0};

                    for (DocumentSnapshot specialtyDoc : specialtySnapshots) {
                        String specialtyId = specialtyDoc.getId(); 
                        String specialtyName = specialtyDoc.getString("name");

                        db.collection("specialties")
                                .document(specialtyId)
                                .collection("services")
                                .get()
                                .addOnSuccessListener(serviceSnapshots -> {
                                    for (DocumentSnapshot serviceDoc : serviceSnapshots) {
                                        String serviceName = serviceDoc.getString("name");
                                        if (serviceName != null) {
                                            appointmentTypes.add(serviceName);
                                            Map<String, Object> serviceData = new HashMap<>();
                                            serviceData.put("serviceName", serviceName);
                                            serviceData.put("specialty", specialtyId);
                                            serviceData.put("specialtyName", specialtyName);
                                            serviceData.put("serviceId", serviceDoc.getId());
                                            servicesListWithSpecialty.add(serviceData);
                                        }
                                    }
                                    processedCount[0]++;
                                    if (processedCount[0] == totalSpecialties) {
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
        Collections.sort(appointmentTypes);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, appointmentTypes);
        spinnerAppointmentType.setAdapter(adapter);
    }

    private void fetchDoctorsByType(String appointmentType) {
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

        String specialty = null;
        for (Map<String, Object> serviceData : servicesListWithSpecialty) {
            if (appointmentType.equals(serviceData.get("serviceName"))) {
                specialty = (String) serviceData.get("specialty");
                selectedSpecialty = specialty;
                break;
            }
        }

        if (specialty == null) {
            Toast.makeText(this, "Could not determine specialty for this service.", Toast.LENGTH_SHORT).show();
            return;
        }

        doctorsQuery = db.collection("doctors")
                .whereEqualTo("specialty", specialty)
                .whereEqualTo("is_active", true);

        promptDoctorSelectionCard(specialty);
        Toast.makeText(this, "Please select a doctor", Toast.LENGTH_LONG).show();
    }

    private void resetDoctorCardToDefault() {
        View doctorView = LayoutInflater.from(this).inflate(R.layout.doctor_card_content, null);
        ImageView doctorImageView = doctorView.findViewById(R.id.doctorImage);
        TextView doctorNameText = doctorView.findViewById(R.id.doctorName);
        TextView doctorSpecialtyText = doctorView.findViewById(R.id.doctorSpecialty);

        doctorNameText.setText("Select Doctor");
        doctorSpecialtyText.setText("Tap to choose");
        doctorImageView.setImageResource(R.drawable.ic_doctor_placeholder); 

        doctorCard.removeAllViews();
        doctorCard.addView(doctorView);
    }

    private void promptDoctorSelectionCard(String category) {
        View doctorView = LayoutInflater.from(this).inflate(R.layout.doctor_card_content, null);
        ImageView doctorImageView = doctorView.findViewById(R.id.doctorImage);
        TextView doctorNameText = doctorView.findViewById(R.id.doctorName);
        TextView doctorSpecialtyText = doctorView.findViewById(R.id.doctorSpecialty);

        doctorNameText.setText("Select Your Doctor");
        doctorSpecialtyText.setText("Specialty: " + category);
        doctorImageView.setImageResource(R.drawable.ic_doctor_placeholder);

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
            selectedDoctor = doctor;
            selectedDoctorId = doctor.getId();

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
                                String dayOfWeek = convertDayAbbrToFull(parts[1]);
                                if (dayOfWeek != null && !availableDaysForCategory.contains(dayOfWeek)) {
                                    availableDaysForCategory.add(dayOfWeek);
                                }
                            }
                        }
                        selectedCategorySchedule = formatScheduleDays(availableDaysForCategory);
                        db.collection("doctors").document(selectedDoctorId).get()
                                .addOnSuccessListener(this::updateDoctorCard);
                    });

            selectedDate = null;
            selectedTime = null;
            textSelectedDate.setText("No date selected");
            gridMorning.removeAllViews();
            gridAfternoon.removeAllViews();
            alertDialog.dismiss();
        };

        doctorAdapter = new DoctorAdapter(this, listener, doctorsQuery, R.layout.item_doctor_no_button);
        recyclerView.setAdapter(doctorAdapter);
        buttonCloseDialog.setOnClickListener(v -> alertDialog.dismiss());
        if (alertDialog.getWindow() != null) alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialog.show();
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
        String[] dayOrder = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        List<String> sortedDays = new ArrayList<>();
        for (String day : dayOrder) if (days.contains(day)) sortedDays.add(day);
        return String.join(", ", sortedDays);
    }

    private void updateDoctorCard(DocumentSnapshot doctor) {
        View doctorView = LayoutInflater.from(this).inflate(R.layout.doctor_card_content, null);
        doctorImageView = doctorView.findViewById(R.id.doctorImage);
        doctorNameText = doctorView.findViewById(R.id.doctorName);
        doctorSpecialtyText = doctorView.findViewById(R.id.doctorSpecialty);

        doctorNameText.setText(doctor.getString("name"));
        doctorSpecialtyText.setText(doctor.getString("specialty") + " | " + selectedCategorySchedule);

        String imageUrl = doctor.getString("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_doctor_placeholder).into(doctorImageView);
        }

        doctorCard.removeAllViews();
        doctorCard.addView(doctorView);
    }

    private void showDatePicker() {
        if (selectedDoctor == null) {
            Toast.makeText(this, "Please select a doctor first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, day);

            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.US);
            if (!availableDaysForCategory.contains(dayFormat.format(selectedCalendar.getTime()))) {
                Toast.makeText(this, "Service is not available on that day.", Toast.LENGTH_LONG).show();
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDate = sdf.format(selectedCalendar.getTime());
            textSelectedDate.setText(new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(selectedCalendar.getTime()));
            fetchTimeSlots(selectedDoctorId, selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void fetchTimeSlots(String doctorId, String date) {
        gridMorning.removeAllViews();
        gridAfternoon.removeAllViews();

        SimpleDateFormat dateDbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateDbFormat.parse(date));
            String dayAbbr = new SimpleDateFormat("EEE", Locale.US).format(cal.getTime()).toUpperCase(Locale.US);
            String scheduleDocId = doctorId + "_" + dayAbbr;

            db.collection("clinic_schedules").document(scheduleDocId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        generateTimeSlots(doc.getString("start_time"), doc.getString("end_time"), 30);
                        blockUnavailableSlots(doctorId, date);
                    }
                });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void generateTimeSlots(String start, String end, int duration) {
        SimpleDateFormat time24 = new SimpleDateFormat("HH:mm", Locale.US);
        SimpleDateFormat time12 = new SimpleDateFormat("h:mm a", Locale.US);
        try {
            Calendar curr = Calendar.getInstance(); curr.setTime(time24.parse(start));
            Calendar e = Calendar.getInstance(); e.setTime(time24.parse(end));
            while (curr.before(e)) {
                createTimeSlotButton(time24.format(curr.getTime()), time12.format(curr.getTime()), curr.get(Calendar.AM_PM));
                curr.add(Calendar.MINUTE, duration);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void createTimeSlotButton(String t24, String t12, int ampm) {
        Button b = new Button(this);
        b.setText(t12); b.setTag(t24);
        b.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_available));
        b.setAllCaps(false);
        b.setOnClickListener(this::onTimeSlotClicked);
        
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0; p.height = (int)(40 * getResources().getDisplayMetrics().density);
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(6, 6, 6, 6);
        b.setLayoutParams(p);
        if (ampm == Calendar.AM) gridMorning.addView(b); else gridAfternoon.addView(b);
    }

    private void onTimeSlotClicked(View v) {
        if (selectedTimeButton != null) {
            selectedTimeButton.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_available));
            selectedTimeButton.setTextColor(Color.BLACK);
        }
        selectedTimeButton = (Button) v;
        selectedTimeButton.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_selected));
        selectedTimeButton.setTextColor(Color.WHITE);
        selectedTime = selectedTimeButton.getTag().toString();
    }

    private void checkPatientDailyAppointmentLimit() {
        if (mAuth.getCurrentUser() == null || selectedDoctorId == null || selectedDate == null || selectedTime == null) {
            Toast.makeText(this, "Complete all details.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("appointments").whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .whereEqualTo("date", selectedDate).whereIn("status", List.of("pending", "confirmed"))
                .get().addOnSuccessListener(q -> {
                    if (!q.isEmpty()) showActiveDialog();
                    else showConfirmationDialog();
                });
    }

    private void performBooking() {
        db.collection("patients").whereEqualTo("userId", mAuth.getCurrentUser().getUid()).limit(1).get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) {
                        String patientId = q.getDocuments().get(0).getId();
                        saveAppointmentToFirestore(mAuth.getCurrentUser().getUid(), patientId);
                    }
                });
    }

    private void saveAppointmentToFirestore(String authUid, String patientDocId) {
        String apptId = "APPT_" + System.currentTimeMillis();
        Map<String, Object> a = new HashMap<>();
        a.put("userId", authUid);
        a.put("patientId", patientDocId);
        a.put("doctorId", selectedDoctorId);
        a.put("doctorName", selectedDoctor.getName());
        a.put("specialty", selectedDoctor.getSpecialty());
        a.put("appointmentType", selectedAppointmentType);
        a.put("date", selectedDate);
        a.put("time", selectedTime);
        a.put("timestamp", new Date());
        a.put("status", "pending");

        db.collection("appointments").document(apptId).set(a).addOnSuccessListener(v -> {
            showSuccessDialog();
            // CRITICAL FIX: Send the Patient's FIRESTORE ID (patientDocId) instead of Auth UID
            sendNotificationTrigger(patientDocId, "A patient", selectedDoctorId);
        });
    }

    private void sendNotificationTrigger(String patientDocId, String name, String docId) {
        String url = "https://sttherese-api.onrender.com/send_notification.php";
        StringRequest req = new StringRequest(Request.Method.POST, url, r -> {}, e -> {}) {
            @Override protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("action", "appointment_booked");
                p.put("uid", patientDocId); // Now sending Firestore ID
                p.put("patient_name", name);
                p.put("doctor_id", docId);
                p.put("date", selectedDate);
                p.put("time", selectedTime);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void blockUnavailableSlots(String doctorId, String date) {
        db.collection("appointments").whereEqualTo("doctorId", doctorId).whereEqualTo("date", date).get()
                .addOnSuccessListener(q -> {
                    List<String> booked = new ArrayList<>();
                    for (DocumentSnapshot d : q) {
                        if (List.of("pending", "confirmed").contains(d.getString("status"))) {
                            booked.add(d.getString("time"));
                        }
                    }
                    applyBlocksToUI(booked);
                });
    }

    private void applyBlocksToUI(List<String> booked) {
        for (int i = 0; i < gridMorning.getChildCount(); i++) {
            Button b = (Button) gridMorning.getChildAt(i);
            if (booked.contains(b.getTag().toString())) {
                b.setEnabled(false); b.setText("BOOKED");
                b.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_unavailable));
            }
        }
        for (int i = 0; i < gridAfternoon.getChildCount(); i++) {
            Button b = (Button) gridAfternoon.getChildAt(i);
            if (booked.contains(b.getTag().toString())) {
                b.setEnabled(false); b.setText("BOOKED");
                b.setBackground(ContextCompat.getDrawable(this, R.drawable.time_slot_unavailable));
            }
        }
    }

    private void showSuccessDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this).setView(getLayoutInflater().inflate(R.layout.dialog_appointment_success, null)).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        new android.os.Handler().postDelayed(() -> { dialog.dismiss(); finish(); }, 2000);
    }

    private void showActiveDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_active_appointment, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        v.findViewById(R.id.buttonOk).setOnClickListener(view -> dialog.dismiss());
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void showConfirmationDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_appointment_confirm, null);
        ((TextView)v.findViewById(R.id.confirmAppointmentType)).setText(selectedAppointmentType);
        ((TextView)v.findViewById(R.id.confirmDoctorName)).setText(selectedDoctor.getName());
        ((TextView)v.findViewById(R.id.confirmTime)).setText(selectedTime);
        ((TextView)v.findViewById(R.id.confirmDate)).setText(selectedDate);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        v.findViewById(R.id.confirmButton).setOnClickListener(view -> { dialog.dismiss(); performBooking(); });
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }
}
