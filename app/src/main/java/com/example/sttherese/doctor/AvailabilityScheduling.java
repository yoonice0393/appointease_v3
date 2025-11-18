package com.example.sttherese.doctor;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_availability_scheduling);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupListeners();
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

        spinnerException.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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
                spinnerException.setSelection(0); // set to first maybe "modified" but we override below
                btnAllDay.setBackgroundTintList(getResources().getColorStateList(R.color.red_primary, null));

                slot.startTime = null;
                slot.endTime = null;
                slot.exceptionType = "blocked"; // automatic
                spinnerException.setVisibility(View.GONE);
            } else {
                etStartTime.setText("");
                etEndTime.setText("");
                etStartTime.setEnabled(true);
                etEndTime.setEnabled(true);
                btnAllDay.setBackgroundTintList(getResources().getColorStateList(R.color.brown_text, null));
                // Restore spinner
                spinnerException.setVisibility(View.VISIBLE);
                // default to "modified" if not set
                if (slot.exceptionType == null || slot.exceptionType.equals("blocked")) {
                    slot.exceptionType = "modified";
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
                    String display = displayDateFormat.format(calendar.getTime());
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
        if (!validateSlots()) {
            Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show();
            return;
        }
        saveSlotsToFirestore();
    }

    private boolean validateSlots() {
        if (availabilitySlots.isEmpty()) return false;
        for (AvailabilitySlot slot : availabilitySlots) {
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
    private void saveSlotsToFirestore() {
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

        final int total = availabilitySlots.size();
        final int[] completed = {0};
        final int[] failed = {0};

        for (AvailabilitySlot slot : availabilitySlots) {
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
                        completed[0]++;
                        if (completed[0] + failed[0] == total) {
                            progress.dismiss();
                            if (failed[0] == 0) {
                                Toast.makeText(AvailabilityScheduling.this, "Schedule exception added successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(AvailabilityScheduling.this, "Saved with some failures.", Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        failed[0]++;
                        completed[0]++;
                        if (completed[0] + failed[0] == total) {
                            progress.dismiss();
                            Toast.makeText(AvailabilityScheduling.this, "Failed to save some slots: " + failed[0], Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    /**
     * Replace with real doctor id retrieval.
     */
    private String getCurrentDoctorId() {
        // TODO: Replace with actual logic (e.g., FirebaseAuth.getInstance().getCurrentUser().getUid())
        return "D001";
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
