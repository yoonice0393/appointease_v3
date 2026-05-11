package com.example.sttherese.patient.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sttherese.DateFormatter;
import com.example.sttherese.Privacy_Policy;
import com.example.sttherese.R;
import com.example.sttherese.SignInPage;
import com.example.sttherese.Terms_Conditions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    ImageView backBtn;
    MaterialButton logoutBtn, termsBtn, privacyBtn, editMedicalHistoryBtn;
    TextView dialogTitle, nameHolder, genderHolder, dobHolder, emailHolder, mobileHolder,
            addressHolder, usernameHolder, patientIdHolder, nameCard, emailCard, addressCard;
    TextView medicalConditionHolder, currentMedicationHolder, allergiesHolder, familyMedicalHistoryHolder;
    TextView showInfoLink;
    Button btnYes, btnNo;
    View view;


    private String userId;
    private String patientDocumentId;
    private final Map<String, Object> currentMedicalHistory = new HashMap<>();

    private boolean isInfoVisible = false;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        nameHolder = findViewById(R.id.textViewNameHolder);
        nameCard = findViewById(R.id.textViewNameCard);
        emailCard = findViewById(R.id.textViewEmailCard);
        addressCard = findViewById(R.id.textViewAddressCard);
        genderHolder = findViewById(R.id.textViewGenderHolder);
        dobHolder = findViewById(R.id.textViewDOBHolder);
        emailHolder = findViewById(R.id.textViewEmailHolder);
        mobileHolder = findViewById(R.id.textViewMobileHolder);
        addressHolder = findViewById(R.id.textViewAddressHolder);
        medicalConditionHolder = findViewById(R.id.textViewMedicalConditionHolder);
        currentMedicationHolder = findViewById(R.id.textViewCurrentMedicationHolder);
        allergiesHolder = findViewById(R.id.textViewAllergiesHolder);
        familyMedicalHistoryHolder = findViewById(R.id.textViewFamilyMedicalHistoryHolder);
        usernameHolder = findViewById(R.id.textViewUsernameHolder);
//        passwordHolder = findViewById(R.id.textViewPasswordHolder);
        patientIdHolder = findViewById(R.id.textViewPatientIdHolder);
        showInfoLink = findViewById(R.id.textViewShowInfo);
        backBtn = findViewById(R.id.buttonBack);
        privacyBtn = findViewById(R.id.buttonPrivacy);
        termsBtn = findViewById(R.id.buttonTerms);
        logoutBtn = findViewById(R.id.buttonLogout);
        editMedicalHistoryBtn = findViewById(R.id.buttonEditMedicalHistory);

        // Get userId from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getString("user_doc_id", null);


        if (userId != null) {
            fetchProfile(userId);
            hideAccountInfo();
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
        }

        showInfoLink.setOnClickListener(v -> {
            if (isInfoVisible) {
                hideAccountInfo();
            } else {
                showPasswordDialog();
            }
        });
        termsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, Terms_Conditions.class);
            startActivity(intent);
        });
        privacyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, Privacy_Policy.class);
            startActivity(intent);
        });


        backBtn.setOnClickListener(v -> onBackPressed());

        logoutBtn.setOnClickListener(v -> showLogoutDialog());
        editMedicalHistoryBtn.setOnClickListener(v -> showMedicalHistoryUpdateDialog());
    }

    private void showLogoutDialog() {
        view = getLayoutInflater().inflate(R.layout.dialog_yes_no, null);
        dialogTitle = view.findViewById(R.id.dialog_title);
        btnYes = view.findViewById(R.id.btn_yes);
        btnNo = view.findViewById(R.id.btn_no);

        dialogTitle.setText("Are you sure you want to log out?");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(ProfileActivity.this, SignInPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showPasswordDialog() {
        // 1. Create the new Bottom Sheet Fragment
        PasswordEntryBottomSheet bottomSheet = new PasswordEntryBottomSheet();

        // 2. Set the listener to handle success callback
        bottomSheet.setPasswordVerificationListener(new PasswordEntryBottomSheet.PasswordVerificationListener() {
            @Override
            public void onVerificationSuccess() {
                // This method is called from the BottomSheet on successful verification
                showAccountInfo();
            }
        });

        // 3. Show the bottom sheet using FragmentManager
        bottomSheet.show(getSupportFragmentManager(), "PasswordEntryBottomSheet");
    }



    private void hideAccountInfo() {
        usernameHolder.setText("******");
//        passwordHolder.setText("**********");
        if (patientIdHolder != null) patientIdHolder.setText("*****");
        showInfoLink.setText("Show Information");
        isInfoVisible = false;
    }

    private void showAccountInfo() {
        showInfoLink.setText("Hide Information");
        isInfoVisible = true;
        fetchProfile(userId);
    }

    private void fetchProfile(String authUid) {
        if (authUid == null) {
            Toast.makeText(this, "User ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Fetch from 'users' collection using the Auth UID as the document ID
        db.collection("users").document(authUid).get().addOnSuccessListener(userSnap -> {
            if (userSnap.exists()) {
                String email = userSnap.getString("email");
                String roleType = userSnap.getString("user_role_type");
                emailHolder.setText(email != null ? email : "N/A");
                emailCard.setText(email != null ? email : "N/A");

                // 2. Fetch the 'patients' document by querying the 'userId' field
                db.collection("patients")
                        .whereEqualTo("userId", authUid) // Find the patient document linked to this Auth UID
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                // Found the matching patient document
                                com.google.firebase.firestore.DocumentSnapshot patientSnap = querySnapshot.getDocuments().get(0);

                                // Extract the document ID (the custom patientId)
                                String patientId = patientSnap.getId();
                                patientDocumentId = patientId;

                                // --- Patient Details Extraction ---

                                String firstName = capitalizeWords(patientSnap.getString("first_name"));
                                String middleName = patientSnap.getString("middle_name") != null ?
                                        capitalizeWords(patientSnap.getString("middle_name")) + " " : "";
                                String lastName = capitalizeWords(patientSnap.getString("last_name"));
                                String fullName = firstName + " " + middleName + lastName;

                                nameHolder.setText(fullName.trim());
                                nameCard.setText(fullName.trim());

                                genderHolder.setText(capitalizeFirst(patientSnap.getString("gender")));

                                String dob = patientSnap.getString("dob");
                                dobHolder.setText(formatDate(dob));

                                mobileHolder.setText(patientSnap.getString("contact"));
                                addressHolder.setText(capitalizeWords(patientSnap.getString("address")));
                                addressCard.setText(capitalizeWords(patientSnap.getString("address")));
                                displayMedicalHistory(patientSnap.get("medical_history"));

                                if (isInfoVisible) {
                                    usernameHolder.setText(email != null ? email.split("@")[0] : "");

                                    // Display the custom patient ID
                                    if (patientIdHolder != null) {
                                        patientIdHolder.setText(patientId);
                                    }


                                } else {
                                    hideAccountInfo();
                                }
                            } else {
                                Toast.makeText(this, "Patient details not found (Query failed).", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error querying patient details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "User account not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private String formatDate(String dateString) {
        return DateFormatter.formatToFullDate(dateString);
    }

    private void displayMedicalHistory(Object medicalHistoryObject) {
        if (!(medicalHistoryObject instanceof Map)) {
            currentMedicalHistory.clear();
            setMedicalHistoryText("N/A", "N/A", "N/A", "N/A");
            return;
        }

        Map<?, ?> medicalHistory = (Map<?, ?>) medicalHistoryObject;
        currentMedicalHistory.clear();
        currentMedicalHistory.put("condition", getRawMedicalHistoryText(medicalHistory, "condition"));
        currentMedicalHistory.put("medication", getRawMedicalHistoryText(medicalHistory, "medication"));
        currentMedicalHistory.put("allergies", getRawMedicalHistoryText(medicalHistory, "allergies"));
        currentMedicalHistory.put("family_history", getRawMedicalHistoryText(medicalHistory, "family_history"));

        setMedicalHistoryText(
                getMedicalHistoryText(medicalHistory, "condition"),
                getMedicalHistoryText(medicalHistory, "medication"),
                getMedicalHistoryText(medicalHistory, "allergies"),
                getMedicalHistoryText(medicalHistory, "family_history")
        );
    }

    private String getRawMedicalHistoryText(Map<?, ?> medicalHistory, String key) {
        Object value = medicalHistory.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private String getMedicalHistoryText(Map<?, ?> medicalHistory, String key) {
        Object value = medicalHistory.get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            return "N/A";
        }
        return capitalizeWords(value.toString());
    }

    private void setMedicalHistoryText(String condition, String medication, String allergies, String familyHistory) {
        medicalConditionHolder.setText(condition);
        currentMedicationHolder.setText(medication);
        allergiesHolder.setText(allergies);
        familyMedicalHistoryHolder.setText(familyHistory);
    }

    private void showMedicalHistoryUpdateDialog() {
        if (patientDocumentId == null || patientDocumentId.trim().isEmpty()) {
            Toast.makeText(this, "Patient profile is still loading.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_medical_history, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        ImageView closeBtn = dialogView.findViewById(R.id.buttonCloseMedicalHistory);
        EditText conditionInput = dialogView.findViewById(R.id.editMedicalCondition);
        EditText medicationInput = dialogView.findViewById(R.id.editCurrentMedication);
        EditText allergiesInput = dialogView.findViewById(R.id.editAllergies);
        EditText familyHistoryInput = dialogView.findViewById(R.id.editFamilyMedicalHistory);
        CheckBox saveToProfileCheck = dialogView.findViewById(R.id.checkSaveToProfile);
        MaterialButton saveBtn = dialogView.findViewById(R.id.buttonSaveMedicalHistory);

        saveToProfileCheck.setVisibility(View.GONE);
        conditionInput.setText(getCurrentMedicalHistoryValue("condition"));
        medicationInput.setText(getCurrentMedicalHistoryValue("medication"));
        allergiesInput.setText(getCurrentMedicalHistoryValue("allergies"));
        familyHistoryInput.setText(getCurrentMedicalHistoryValue("family_history"));

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        saveBtn.setOnClickListener(v -> {
            Map<String, Object> updatedMedicalHistory = new HashMap<>();
            updatedMedicalHistory.put("condition", conditionInput.getText().toString().trim());
            updatedMedicalHistory.put("medication", medicationInput.getText().toString().trim());
            updatedMedicalHistory.put("allergies", allergiesInput.getText().toString().trim());
            updatedMedicalHistory.put("family_history", familyHistoryInput.getText().toString().trim());
            updatedMedicalHistory.put("updated_at", new Date());

            saveBtn.setEnabled(false);
            db.collection("patients")
                    .document(patientDocumentId)
                    .update(
                            "medical_history", updatedMedicalHistory,
                            "medical_history_updated_at", new Date()
                    )
                    .addOnSuccessListener(unused -> {
                        displayMedicalHistory(updatedMedicalHistory);
                        Toast.makeText(this, "Medical history updated.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        saveBtn.setEnabled(true);
                        Toast.makeText(this, "Failed to update medical history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private String getCurrentMedicalHistoryValue(String key) {
        Object value = currentMedicalHistory.get(key);
        return value == null ? "" : value.toString();
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

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        text = text.trim().toLowerCase();
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
