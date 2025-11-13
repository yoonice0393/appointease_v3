package com.example.sttherese.doctor;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sttherese.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AvailabilityScheduling extends AppCompatActivity {

    private LinearLayout slotsContainer;
    private ImageView btnAdd;
    private MaterialButton btnConfirm;
    private ImageView closeButton;
    private List<AvailabilitySlot> availabilitySlots = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_availability_scheduling);

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

        btnConfirm.setOnClickListener(v -> confirmAvailability());

        closeButton.setOnClickListener(v -> finish());
    }

    private void addInitialSlot() {
        addNewSlot();
    }

    private void addNewSlot() {
        View slotView = LayoutInflater.from(this)
                .inflate(R.layout.item_availability_slot, slotsContainer, false);

        TextInputEditText etDate = slotView.findViewById(R.id.etDate);
        TextInputEditText etTime = slotView.findViewById(R.id.etTime);
        MaterialButton btnAllDay = slotView.findViewById(R.id.btnAllDay);

        AvailabilitySlot slot = new AvailabilitySlot();
        availabilitySlots.add(slot);

        // Date picker
        etDate.setOnClickListener(v -> showDatePicker(etDate, slot));

        // Time picker
        etTime.setOnClickListener(v -> showTimePicker(etTime, slot));

        // All Day toggle
        btnAllDay.setOnClickListener(v -> {
            slot.isAllDay = !slot.isAllDay;
            if (slot.isAllDay) {
                etTime.setText("All Day");
                etTime.setEnabled(false);
                btnAllDay.setBackgroundTintList(
                        getResources().getColorStateList(R.color.red_primary, null));
            } else {
                etTime.setText("");
                etTime.setEnabled(true);
                btnAllDay.setBackgroundTintList(
                        getResources().getColorStateList(R.color.brown_text, null));
            }
        });

        slotsContainer.addView(slotView);
    }

    private void showDatePicker(TextInputEditText etDate, AvailabilitySlot slot) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                    String dateString = sdf.format(calendar.getTime());
                    etDate.setText(dateString);
                    slot.date = dateString;
                    slot.calendar = (Calendar) calendar.clone();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker(TextInputEditText etTime, AvailabilitySlot slot) {
        Calendar calendar = Calendar.getInstance();

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    Calendar startTime = Calendar.getInstance();
                    startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    startTime.set(Calendar.MINUTE, minute);

                    Calendar endTime = (Calendar) startTime.clone();
                    endTime.add(Calendar.HOUR_OF_DAY, 12);

                    SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    String timeString = sdf.format(startTime.getTime()) + " to " +
                            sdf.format(endTime.getTime());
                    etTime.setText(timeString);
                    slot.startTime = sdf.format(startTime.getTime());
                    slot.endTime = sdf.format(endTime.getTime());
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );

        timePickerDialog.show();
    }

    private void confirmAvailability() {
        // Validate all slots
        boolean allValid = true;
        for (AvailabilitySlot slot : availabilitySlots) {
            if (slot.date == null || slot.date.isEmpty()) {
                allValid = false;
                break;
            }
            if (!slot.isAllDay && (slot.startTime == null || slot.startTime.isEmpty())) {
                allValid = false;
                break;
            }
        }

        if (!allValid) {
            Toast.makeText(this, "Please fill in all fields correctly",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Save availability slots to database or send to server
        // For now, just show success message
        StringBuilder message = new StringBuilder("Availability Set:\n");
        for (AvailabilitySlot slot : availabilitySlots) {
            message.append(slot.date).append(" - ");
            if (slot.isAllDay) {
                message.append("All Day\n");
            } else {
                message.append(slot.startTime).append(" to ").append(slot.endTime).append("\n");
            }
        }

        Toast.makeText(this, "Availability saved successfully!", Toast.LENGTH_LONG).show();

        // TODO: Save to Firebase/Database
        // saveToDatabase();

        finish();
    }

    // Inner class to hold slot data
    private static class AvailabilitySlot {
        String date;
        String startTime;
        String endTime;
        boolean isAllDay = false;
        Calendar calendar;
    }
}