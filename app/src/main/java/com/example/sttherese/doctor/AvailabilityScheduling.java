package com.example.sttherese.doctor;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sttherese.R;
import com.example.sttherese.DateFormatter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AvailabilityScheduling Activity
 * - Uses Option B: store schedule_exceptions with ID = doctorId_yyyy-MM-dd
 * - Each slot has Date, Start Time, End Time, Exception Type (modified/added/blocked)
 * - "All Day" button stays: when ON -> exception_type = "blocked" and time fields disabled
 */
public class AvailabilityScheduling extends AppCompatActivity {

    private LinearLayout slotsContainer;
    private ImageView btnAdd;
    private MaterialButton btnConfirm;
    private ImageView closeButton;

    private final List<AvailabilitySlot> availabilitySlots = new ArrayList<>();

    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat firestoreDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat firestoreTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat displayTimeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    private FirebaseFirestore db;
    private String currentDoctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_availability_scheduling);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupListeners();
        loadCurrentDoctorId();
        addInitialSlot();
    }

    private void initializeViews() {
        slotsContainer = findViewById(R.id.slotsContainer);
        btnAdd = findViewById(R.id.btnAdd);
        btnConfirm = findViewById(R.id.btnConfirm);
        closeButton = findViewById(R.id.closeButton);
    }

    private void setupListeners() {
        btnAdd.setOnClickListener(v -> addNewSlot());
        btnConfirm.setOnClickListener(v -> onConfirmClick());
        closeButton.setOnClickListener(v -> finish());
    }

    private void addInitialSlot() {
        addNewSlot();
    }

    private void addNewSlot() {
        View slotView = LayoutInflater.from(this).inflate(R.layout.item_availability_slot, slotsContainer, false);

        TextInputEditText etDate = slotView.findViewById(R.id.etDate);
        TextInputEditText etStartTime = slotView.findViewById(R.id.etStartTime);
        TextInputEditText etEndTime = slotView.findViewById(R.id.etEndTime);
        MaterialButton btnAllDay = slotView.findViewById(R.id.btnAllDay);
        Spinner spinnerException = slotView.findViewById(R.id.spinnerException);

        final AvailabilitySlot slot = new AvailabilitySlot();
        availabilitySlots.add(slot);

        // Configure spinner (only shown when not all-day)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.exception_types_array,  // we'll include this resource below
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerException.setAdapter(adapter);
        slot.exceptionType = "modified";

        spinnerException.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (slot.isAllDay) return;
                String val = parent.getItemAtPosition(position).toString();
                slot.exceptionType = val.toLowerCase(Locale.ROOT); // "modified" or "added"
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Date picker
        etDate.setOnClickListener(v -> showDatePicker(etDate, slot));

        // Start time (separate field)
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime, slot, true, etEndTime));

        // End time (separate field)
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime, slot, false, null));

        // All Day toggle - stays per Option A
        btnAllDay.setOnClickListener(v -> {
            slot.isAllDay = !slot.isAllDay;
            if (slot.isAllDay) {
                // Disable time fields and set spinner to blocked
                etStartTime.setText("All Day");
                etEndTime.setText("");
                etStartTime.setEnabled(false);
                etEndTime.setEnabled(false);
                btnAllDay.setBackgroundTintList(getResources().getColorStateList(R.color.red_primary, null));

                slot.startTime = null;
                slot.endTime = null;
                slot.exceptionType = "blocked"; // automatic
                spinnerException.setEnabled(false);
                spinnerException.setAlpha(0.45f);
            } else {
                etStartTime.setText("");
                etEndTime.setText("");
                etStartTime.setEnabled(true);
                etEndTime.setEnabled(true);
                btnAllDay.setBackgroundTintList(getResources().getColorStateList(R.color.brown_text, null));
                spinnerException.setEnabled(true);
                spinnerException.setAlpha(1f);
                // default to "modified" if not set
                if (slot.exceptionType == null || slot.exceptionType.equals("blocked")) {
                    slot.exceptionType = "modified";
                    spinnerException.setSelection(0);
                }
            }
        });

        // Long-press to remove slot
        slotView.setOnLongClickListener(v -> {
            availabilitySlots.remove(slot);
            slotsContainer.removeView(slotView);
            Toast.makeText(AvailabilityScheduling.this, "Slot removed", Toast.LENGTH_SHORT).show();
            return true;
        });

        slotsContainer.addView(slotView);
    }

    /**
     * Shows DatePicker and saves both display & firestore formats.
     */
    private void showDatePicker(TextInputEditText etDate, AvailabilitySlot slot) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    String display = com.example.sttherese.DateFormatter.formatToFullDate(firestoreDateFormat.format(calendar.getTime()));
                    etDate.setText(display);
                    slot.dateDisplay = display;
                    slot.dateFirestore = firestoreDateFormat.format(calendar.getTime());
                    slot.calendar = (Calendar) calendar.clone();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    /**
     * Shows TimePicker for start or end time.
     *
     * If isStart==true and endField is provided, after selecting start we do NOT auto-pick end.
     * The user selects end separately (Option 3).
     */
    private void showTimePicker(TextInputEditText targetField, AvailabilitySlot slot, boolean isStart, TextInputEditText endField) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    chosen.set(Calendar.MINUTE, minute);

                    String display = displayTimeFormat.format(chosen.getTime());
                    String firestoreTime = firestoreTimeFormat.format(chosen.getTime());

                    targetField.setText(display);

                    if (isStart) {
                        slot.startTime = firestoreTime;
                    } else {
                        slot.endTime = firestoreTime;
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void onConfirmClick() {
        List<AvailabilitySlot> slotsToSave = getSlotsToSave();
        if (!validateSlots(slotsToSave)) {
            Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show();
            return;
        }
        saveSlotsToFirestore(slotsToSave);
    }

    private List<AvailabilitySlot> getSlotsToSave() {
        List<AvailabilitySlot> slotsToSave = new ArrayList<>();
        for (AvailabilitySlot slot : availabilitySlots) {
            if (!isBlankSlot(slot)) {
                slotsToSave.add(slot);
            }
        }
        return slotsToSave;
    }

    private boolean isBlankSlot(AvailabilitySlot slot) {
        return (slot.dateFirestore == null || slot.dateFirestore.isEmpty())
                && (slot.startTime == null || slot.startTime.isEmpty())
                && (slot.endTime == null || slot.endTime.isEmpty())
                && !slot.isAllDay;
    }

    private boolean validateSlots(List<AvailabilitySlot> slotsToSave) {
        if (slotsToSave.isEmpty()) return false;
        for (AvailabilitySlot slot : slotsToSave) {
            if (slot.dateFirestore == null || slot.dateFirestore.isEmpty()) return false;
            if (!slot.isAllDay) {
                if (slot.startTime == null || slot.startTime.isEmpty()) return false;
                if (slot.endTime == null || slot.endTime.isEmpty()) return false;

                // Validate end > start
                try {
                    long s = firestoreTimeFormat.parse(slot.startTime).getTime();
                    long e = firestoreTimeFormat.parse(slot.endTime).getTime();
                    if (e <= s) return false;
                } catch (ParseException e) {
                    return false;
                }

                // exceptionType must be set (modified/added)
                if (slot.exceptionType == null || slot.exceptionType.isEmpty()) return false;
            } else {
                // All-day should have blocked type
                if (!"blocked".equals(slot.exceptionType)) {
                    slot.exceptionType = "blocked";
                }
            }
        }
        return true;
    }

    /**
     * Save to Firestore using Option B doc IDs: doctorId_yyyy-MM-dd
     */
    private void saveSlotsToFirestore(List<AvailabilitySlot> slotsToSave) {
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Saving availability...");
        progress.setCancelable(false);
        progress.show();

        final String doctorId = getCurrentDoctorId();
        if (doctorId == null || doctorId.isEmpty()) {
            progress.dismiss();
            Toast.makeText(this, "Unable to determine doctor ID", Toast.LENGTH_SHORT).show();
            return;
        }

        final int total = slotsToSave.size();
        final int[] completed = {0};
        final int[] failed = {0};

        for (AvailabilitySlot slot : slotsToSave) {
            final String docId = doctorId + "_" + slot.dateFirestore;

            Map<String, Object> data = new HashMap<>();
            data.put("doctor_id", doctorId);
            data.put("date", slot.dateFirestore);
            data.put("is_all_day", slot.isAllDay);
            data.put("exception_type", slot.exceptionType);
            data.put("notes", slot.notes == null ? "" : slot.notes);
            data.put("updated_at", FieldValue.serverTimestamp());

            if (slot.isAllDay) {
                data.put("start_time", null);
                data.put("end_time", null);
            } else {
                data.put("start_time", slot.startTime); // "HH:mm"
                data.put("end_time", slot.endTime);     // "HH:mm"
            }

            db.collection("schedule_exceptions")
                    .document(docId)
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        if (slot.isAllDay && "blocked".equals(slot.exceptionType)) {
                            cancelAppointmentsForBlockedDate(doctorId, slot.dateFirestore, () -> markSlotSaveComplete(progress, completed, failed, total));
                        } else {
                            markSlotSaveComplete(progress, completed, failed, total);
                        }
                    })
                    .addOnFailureListener(e -> {
                        failed[0]++;
                        markSlotSaveComplete(progress, completed, failed, total);
                    });
        }
    }

    private void markSlotSaveComplete(ProgressDialog progress, int[] completed, int[] failed, int total) {
        completed[0]++;
        if (completed[0] == total) {
            progress.dismiss();
            if (failed[0] == 0) {
                Toast.makeText(AvailabilityScheduling.this, "Schedule exception added successfully!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(AvailabilityScheduling.this, "Saved with some failures.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void cancelAppointmentsForBlockedDate(String doctorId, String date, Runnable onComplete) {
        db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("date", date)
                .whereIn("status", Arrays.asList("pending", "approved"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    final int[] remaining = {snapshot.size()};
                    String reason = "Doctor marked this date unavailable.";

                    for (com.google.firebase.firestore.DocumentSnapshot appointment : snapshot.getDocuments()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "cancelled");
                        updates.put("cancelled_by", "doctor");
                        updates.put("cancel_reason", reason);
                        updates.put("cancellation_reason", reason);
                        updates.put("cancelled_at", FieldValue.serverTimestamp());

                        appointment.getReference().update(updates)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        sendBlockedDateNotifications(appointment, date, reason);
                                    }
                                    remaining[0]--;
                                    if (remaining[0] == 0) onComplete.run();
                                });
                    }
                })
                .addOnFailureListener(e -> onComplete.run());
    }

    private void sendBlockedDateNotifications(com.google.firebase.firestore.DocumentSnapshot appointment, String date, String reason) {
        String userId = appointment.getString("userId");
        String appointmentType = appointment.getString("appointmentType");
        String time = appointment.getString("time");
        String displayDate = DateFormatter.formatToFullDate(date);
        String title = "Appointment Cancelled";
        String message = "Your " + (appointmentType != null ? appointmentType : "appointment") +
                " on " + displayDate + (time != null ? " at " + time : "") +
                " was cancelled. Reason: " + reason;

        if (userId != null && !userId.trim().isEmpty()) {
            db.collection("patients")
                    .whereEqualTo("userId", userId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(q -> {
                        if (q != null && !q.isEmpty()) {
                            writePatientNotification(q.getDocuments().get(0).getId(), userId, title, message);
                        } else {
                            writePatientNotification(userId, userId, title, message);
                        }
                    })
                    .addOnFailureListener(e -> writePatientNotification(userId, userId, title, message));
        }

        Map<String, Object> staffNotification = buildNotification(null, title,
                "An appointment on " + displayDate + " was cancelled because the doctor blocked the date.",
                "appointment_cancelled", "cancelled");
        staffNotification.put("appointmentId", appointment.getId());
        staffNotification.put("doctorId", appointment.getString("doctorId"));
        db.collection("staff_notifications").add(staffNotification);
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://appointease-7aa63-default-rtdb.asia-southeast1.firebasedatabase.app");
        database.getReference("staff_notifications").push().setValue(staffNotification);
        writeWebsiteRoleNotifications(staffNotification);
    }

    private void writeWebsiteRoleNotifications(Map<String, Object> notification) {
        db.collection("users")
                .whereIn("user_role_type", Arrays.asList("clinic_staff", "clinic_manager", "admin"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    FirebaseDatabase database = FirebaseDatabase.getInstance("https://appointease-7aa63-default-rtdb.asia-southeast1.firebasedatabase.app");
                    for (com.google.firebase.firestore.DocumentSnapshot userDoc : querySnapshot.getDocuments()) {
                        database.getReference("notifications")
                                .child(userDoc.getId())
                                .push()
                                .setValue(notification);
                    }
                });
    }

    private void writePatientNotification(String notificationOwnerId, String userId, String title, String message) {
        Map<String, Object> notification = buildNotification(userId, title, message, "appointment_cancelled", "cancelled");
        FirebaseDatabase.getInstance("https://appointease-7aa63-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("notifications")
                .child(notificationOwnerId)
                .push()
                .setValue(notification);
    }

    private Map<String, Object> buildNotification(String userId, String title, String message, String type, String status) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("status", status);
        notification.put("isRead", false);
        notification.put("timestamp", System.currentTimeMillis());
        return notification;
    }

    private void loadCurrentDoctorId() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentDoctorId = prefs.getString("doctor_doc_id", null);
        if (currentDoctorId != null && !currentDoctorId.trim().isEmpty()) {
            return;
        }

        String authId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : prefs.getString("user_doc_id", null);

        if (authId == null || authId.trim().isEmpty()) {
            return;
        }

        db.collection("doctors")
                .whereEqualTo("user_id", authId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        currentDoctorId = querySnapshot.getDocuments().get(0).getId();
                        prefs.edit().putString("doctor_doc_id", currentDoctorId).apply();
                    }
                });
    }

    private String getCurrentDoctorId() {
        if (currentDoctorId != null && !currentDoctorId.trim().isEmpty()) {
            return currentDoctorId;
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String doctorId = prefs.getString("doctor_doc_id", null);
        return doctorId != null && !doctorId.trim().isEmpty() ? doctorId : null;
    }

    // -----------------------------
    // Model: AvailabilitySlot
    // -----------------------------
    private static class AvailabilitySlot {
        String dateDisplay;   // for UI
        String dateFirestore; // yyyy-MM-dd for Firestore & doc id
        String startTime;     // HH:mm
        String endTime;       // HH:mm
        boolean isAllDay = false;
        String exceptionType; // "modified", "added", "blocked"
        String notes = "";
        Calendar calendar;
    }
}
