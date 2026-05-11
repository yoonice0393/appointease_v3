package com.example.sttherese.doctor;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sttherese.DateFormatter;
import com.example.sttherese.R;
import com.example.sttherese.models.Appointment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public class AppointmentDetailsActivity extends AppCompatActivity {

    private static final String TAG = "ApptDetailsActivity";
    private FirebaseFirestore db;

    // ... (existing view declarations)
    private TextView tvPatientName, tvDate, tvTime, tvSpecialty, tvBirthday, tvAge, tvAddress, tvMedicalHistory;
    private View statusButton;
    private TextView tvStatusLabel;
    private MaterialButton cancelButton;
    private ImageView closeButton;
    private View progressBar; // Assuming you have a loading indicator
    private Appointment currentAppointment;
    private String patientDocumentId;
    private boolean appointmentMedicalHistoryLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_details);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupListeners();



        String appointmentJson = getIntent().getStringExtra("appointment_json");

        if (appointmentJson != null) {
            Appointment appointment = new Gson().fromJson(appointmentJson, Appointment.class);
            if (appointment != null) {
                currentAppointment = appointment;
                // Populate initial appointment details
                populateDetails(appointment);

                // Fetch patient specific details from the 'patients' collection
                fetchPatientDetails(appointment.getUserId());
                fetchAppointmentMedicalHistory(appointment.getId());
            } else {
                Toast.makeText(this, "Failed to load appointment details.", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Appointment data missing.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {

        tvPatientName = findViewById(R.id.tvPatientName);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvSpecialty = findViewById(R.id.tvSpecialty);
        statusButton = findViewById(R.id.statusButton);
        tvStatusLabel = findViewById(R.id.tvStatusLabel);
        cancelButton = findViewById(R.id.buttonCancelAppointment);
        tvBirthday = findViewById(R.id.tvBirthday);
        tvAge = findViewById(R.id.tvAge);
        tvAddress = findViewById(R.id.tvAddress);
        tvMedicalHistory = findViewById(R.id.tvMedicalHistory);
        closeButton = findViewById(R.id.closeButton);
        // progressBar = findViewById(R.id.progressBar); // If you have one
    }

    private void setupListeners() {
        closeButton.setOnClickListener(v -> finish());
        statusButton.setOnClickListener(v -> showStatusOptions());
        cancelButton.setOnClickListener(v -> showCancelDialog());
    }

    private void populateDetails(Appointment appointment) {
        // Set details already available in the Appointment object
        tvPatientName.setText(safeText(appointment.getPatientName(), "Patient"));

        // --- CHANGE 1: Format Appointment Date ---
        String formattedDate = formatFirestoreDate(appointment.getDate());
        tvDate.setText(formattedDate);

        tvTime.setText(safeText(appointment.getTime(), "N/A"));
        tvSpecialty.setText(safeText(appointment.getSpecialty(), safeText(appointment.getAppointmentType(), "N/A")));
        tvStatusLabel.setText(formatStatus(appointment.getStatus()));

        // Set placeholders for patient data until fetched
        tvBirthday.setText("Loading...");
        tvAge.setText("Loading...");
        tvAddress.setText("Loading...");
        tvMedicalHistory.setText("Loading...");
        updateActionAvailability();
    }

    private void fetchPatientDetails(String authUserId) {
        if (authUserId == null || authUserId.isEmpty()) {
            Log.e(TAG, "Auth User ID is null or empty. Cannot fetch patient details.");
            tvBirthday.setText("N/A");
            tvAge.setText("N/A");
            tvAddress.setText("N/A");
            return;
        }

        // --- CORRECTED LOGIC: Query by the 'userId' field (Auth ID) ---
        db.collection("patients")
                .whereEqualTo("userId", authUserId) // This matches the Auth ID to the field in the patient document
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (!querySnapshot.isEmpty()) {
                        // Get the first (and should be only) matching patient document
                        Map<String, Object> patientData = querySnapshot.getDocuments().get(0).getData();
                        patientDocumentId = querySnapshot.getDocuments().get(0).getId();

                        // Extract data based on your Firebase screenshot
                        String dob = (String) patientData.get("dob");
                        // Firestore reads integer numbers as Long
                        Object ageObj = patientData.get("age");
                        String address = (String) patientData.get("address");
                        Object medicalHistory = patientData.get("medical_history");
                        String firstName = (String) patientData.get("first_name");
                        String lastName = (String) patientData.get("last_name");
                        String name = (String) patientData.get("name");
                        String fullName = buildPatientName(firstName, lastName, name);

                        if (!fullName.isEmpty()) {
                            tvPatientName.setText(fullName);
                            currentAppointment.setPatientName(fullName);
                        }
                        if (dob != null) {
                            String formattedDob = formatFirestoreDate(dob);
                            tvBirthday.setText(formattedDob);
                        } else {
                            tvBirthday.setText("N/A");
                        }
                        if (ageObj != null) tvAge.setText(String.valueOf(ageObj)); else tvAge.setText("N/A");
                        if (address != null) tvAddress.setText(address); else tvAddress.setText("N/A");
                        if (!appointmentMedicalHistoryLoaded) {
                            tvMedicalHistory.setText(formatMedicalHistory(medicalHistory));
                        }

                    } else {
                        Log.w(TAG, "Patient document not found for userId: " + authUserId);
                        tvBirthday.setText("N/A");
                        tvAge.setText("N/A");
                        tvAddress.setText("N/A");
                        tvMedicalHistory.setText("N/A");
                        Toast.makeText(this, "Patient details not found (Query successful, but empty).", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching patient details: " + e.getMessage());
                    tvBirthday.setText("Error");
                    tvAge.setText("Error");
                    tvAddress.setText("Error");
                    tvMedicalHistory.setText("Error");
                    Toast.makeText(this, "Error fetching patient details.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchAppointmentMedicalHistory(String appointmentId) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            return;
        }

        db.collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        return;
                    }

                    Object medicalHistory = documentSnapshot.get("medical_history");
                    if (medicalHistory instanceof Map) {
                        appointmentMedicalHistoryLoaded = true;
                        tvMedicalHistory.setText(formatMedicalHistory(medicalHistory));
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Unable to fetch appointment medical history", e));
    }

    private String formatMedicalHistory(Object medicalHistoryObject) {
        if (!(medicalHistoryObject instanceof Map)) {
            return "No medical history saved.";
        }

        Map<?, ?> medicalHistory = (Map<?, ?>) medicalHistoryObject;
        if ("N/A".equals(medicalValue(medicalHistory, "condition")) &&
                "N/A".equals(medicalValue(medicalHistory, "medication")) &&
                "N/A".equals(medicalValue(medicalHistory, "allergies")) &&
                "N/A".equals(medicalValue(medicalHistory, "family_history"))) {
            return "No medical history saved.";
        }

        return "Medical Condition: " + medicalValue(medicalHistory, "condition") +
                "\nCurrent Medication: " + medicalValue(medicalHistory, "medication") +
                "\nAllergies: " + medicalValue(medicalHistory, "allergies") +
                "\nFamily Medical History: " + medicalValue(medicalHistory, "family_history");
    }

    private String medicalValue(Map<?, ?> medicalHistory, String key) {
        Object value = medicalHistory.get(key);
        return value == null || value.toString().trim().isEmpty() ? "N/A" : value.toString().trim();
    }

    private void showStatusOptions() {
        if (currentAppointment == null || currentAppointment.getId() == null) {
            Toast.makeText(this, "Appointment ID missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        String status = safeText(currentAppointment.getStatus(), "").toLowerCase(Locale.getDefault());
        if (isClosedStatus(status)) {
            Toast.makeText(this, "This appointment is already " + status + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!"approved".equals(status)) {
            Toast.makeText(this, "Only clinic-approved appointments can be updated by the doctor.", Toast.LENGTH_SHORT).show();
            return;
        }

        PopupMenu menu = new PopupMenu(this, statusButton);
        menu.getMenu().add("Completed");
        menu.setOnMenuItemClickListener(item -> {
            showCompleteDialog();
            return true;
        });
        menu.show();
    }

    private void confirmStatusChange(String status, String note) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_yes_no, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        TextView title = dialogView.findViewById(R.id.dialog_title);
        MaterialButton yes = dialogView.findViewById(R.id.btn_yes);
        MaterialButton no = dialogView.findViewById(R.id.btn_no);

        title.setText("Are you sure you want to change the status?");
        yes.setText("YES");
        no.setText("NO");

        yes.setOnClickListener(v -> {
            dialog.dismiss();
            updateAppointmentStatus(status, note, null);
        });
        no.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showCompleteDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_complete_appointment, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        EditText noteInput = dialogView.findViewById(R.id.editConsultationNote);
        MaterialButton submit = dialogView.findViewById(R.id.buttonSubmitComplete);

        submit.setOnClickListener(v -> {
            String note = noteInput.getText().toString().trim();
            if (note.isEmpty()) {
                noteInput.setError("Consultation note is required");
                return;
            }
            dialog.dismiss();
            confirmStatusChange("completed", note);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showCancelDialog() {
        if (currentAppointment == null || currentAppointment.getId() == null) {
            Toast.makeText(this, "Appointment ID missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_appointment, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        EditText reasonInput = dialogView.findViewById(R.id.editCancelReason);
        MaterialButton submit = dialogView.findViewById(R.id.buttonSubmitCancel);

        submit.setOnClickListener(v -> {
            String reason = reasonInput.getText().toString().trim();
            if (reason.isEmpty()) {
                reasonInput.setError("Reason is required");
                return;
            }
            dialog.dismiss();
            updateAppointmentStatus("cancelled", null, reason);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void updateAppointmentStatus(String status, String consultationNote, String cancelReason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updated_at", FieldValue.serverTimestamp());

        if (consultationNote != null) {
            updates.put("doctor_note", consultationNote);
            updates.put("consultation_note", consultationNote);
            updates.put("completed_at", FieldValue.serverTimestamp());
        }

        if (cancelReason != null) {
            updates.put("cancel_reason", cancelReason);
            updates.put("cancellation_reason", cancelReason);
            updates.put("cancelled_by", "doctor");
            updates.put("cancelled_at", FieldValue.serverTimestamp());
        }

        db.collection("appointments")
                .document(currentAppointment.getId())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    currentAppointment.setStatus(status);
                    tvStatusLabel.setText(formatStatus(status));
                    updateActionAvailability();
                    notifyStatusChange(status, consultationNote, cancelReason);
                    Toast.makeText(this, "Appointment updated.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update appointment: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void notifyStatusChange(String status, String consultationNote, String cancelReason) {
        String patientUserId = currentAppointment.getUserId();
        String title = "Appointment " + formatStatus(status).toLowerCase(Locale.getDefault());
        String message;
        if ("completed".equals(status)) {
            message = "Your appointment on " + formatFirestoreDate(currentAppointment.getDate()) + " at " +
                    safeText(currentAppointment.getTime(), "N/A") + " was marked completed." +
                    (consultationNote != null ? " Note: " + consultationNote : "");
        } else if ("cancelled".equals(status)) {
            message = "Your appointment on " + formatFirestoreDate(currentAppointment.getDate()) + " at " +
                    safeText(currentAppointment.getTime(), "N/A") + " was cancelled by the doctor. Reason: " + cancelReason;
        } else {
            message = "Your appointment status was updated to " + formatStatus(status) + ".";
        }

        if (patientDocumentId != null) {
            writeNotification(patientDocumentId, patientUserId, title, message, status);
        }
        if (patientUserId != null) {
            writeNotification(patientUserId, patientUserId, title, message, status);
        }

        Map<String, Object> staffNotification = new HashMap<>();
        staffNotification.put("title", title);
        staffNotification.put("message", message);
        staffNotification.put("appointmentId", currentAppointment.getId());
        staffNotification.put("doctorId", currentAppointment.getDoctorId());
        staffNotification.put("doctorName", currentAppointment.getDoctorName());
        staffNotification.put("patientName", currentAppointment.getPatientName());
        staffNotification.put("patientUserId", patientUserId);
        staffNotification.put("status", status);
        staffNotification.put("type", "completed".equals(status) ? "appointment_completed" : "appointment_cancelled");
        staffNotification.put("isRead", false);
        staffNotification.put("timestamp", System.currentTimeMillis());

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://appointease-7aa63-default-rtdb.asia-southeast1.firebasedatabase.app");
        writeWebsiteRoleNotifications(staffNotification, false);
        database.getReference("staff_notifications").push().setValue(staffNotification);
        db.collection("staff_notifications").add(staffNotification);
    }

    private void writeWebsiteRoleNotifications(Map<String, Object> notification, boolean managersOnly) {
        java.util.List<String> roles = managersOnly
                ? Arrays.asList("clinic_manager", "admin")
                : Arrays.asList("clinic_staff", "clinic_manager", "admin");

        db.collection("users")
                .whereIn("user_role_type", roles)
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

    private void writeNotification(String recipientId, String patientUserId, String title, String message, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", patientUserId);
        data.put("title", title);
        data.put("message", message);
        data.put("appointmentId", currentAppointment.getId());
        data.put("status", status);
        data.put("type", "appointment_status");
        data.put("isRead", false);
        data.put("timestamp", System.currentTimeMillis());

        DatabaseReference ref = FirebaseDatabase.getInstance("https://appointease-7aa63-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("notifications").child(recipientId).push();
        ref.setValue(data);
    }

    private void updateActionAvailability() {
        if (currentAppointment == null) {
            statusButton.setEnabled(false);
            cancelButton.setEnabled(false);
            return;
        }

        String status = safeText(currentAppointment.getStatus(), "").toLowerCase(Locale.getDefault());
        boolean closed = isClosedStatus(status);
        boolean doctorActionAllowed = "approved".equals(status);
        statusButton.setEnabled(!closed && doctorActionAllowed);
        cancelButton.setEnabled(!closed && doctorActionAllowed);
        cancelButton.setVisibility((!closed && doctorActionAllowed) ? View.VISIBLE : View.GONE);
    }

    private boolean isClosedStatus(String status) {
        return "completed".equals(status) || "cancelled".equals(status) || "denied".equals(status);
    }

    private String formatStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "Pending";
        }
        String normalized = status.trim().toLowerCase(Locale.getDefault());
        return normalized.substring(0, 1).toUpperCase(Locale.getDefault()) + normalized.substring(1);
    }

    private String buildPatientName(String firstName, String lastName, String fallbackName) {
        String combined = (safeText(firstName, "") + " " + safeText(lastName, "")).trim();
        return combined.isEmpty() ? safeText(fallbackName, "") : combined;
    }

    private String safeText(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }


    private String formatFirestoreDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return "N/A";
        }
        // The format Firestore uses (YYYY-MM-DD)
        return DateFormatter.formatToFullDate(dateString);
    }
}
