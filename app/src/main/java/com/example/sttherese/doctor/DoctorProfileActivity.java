package com.example.sttherese.doctor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import com.example.sttherese.R;
import com.example.sttherese.SignInPage;
import com.example.sttherese.Terms_Conditions;
import com.example.sttherese.patient.activities.PasswordEntryBottomSheet;
import com.example.sttherese.patient.activities.ProfileActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DoctorProfileActivity extends AppCompatActivity {



    ImageView backBtn;
    MaterialButton logoutBtn, termsBtn;
    TextView dialogTitle, nameHolder, genderHolder, dobHolder, emailHolder, mobileHolder,
            specialtyHolder, usernameHolder, doctorIdHolder, nameCard, emailCard, specialtyCard;
    TextView showInfoLink;
    Button btnYes, btnNo;
    View view;

    private String userId;

    private boolean isInfoVisible = false;

    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_doctor_profile);
        db = FirebaseFirestore.getInstance();

        initializeView();


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

        backBtn.setOnClickListener(v -> onBackPressed());

        logoutBtn.setOnClickListener(v -> showLogoutDialog());
        termsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(DoctorProfileActivity.this, Terms_Conditions.class);
            startActivity(intent);
        });

    }

    private void initializeView() {
        nameHolder = findViewById(R.id.textViewNameHolder);
        nameCard = findViewById(R.id.textViewNameCard);
        emailCard = findViewById(R.id.textViewEmailCard);
        specialtyCard = findViewById(R.id.textViewSpecialtyCard);
        genderHolder = findViewById(R.id.textViewGenderHolder);
        dobHolder = findViewById(R.id.textViewDOBHolder);
        emailHolder = findViewById(R.id.textViewEmailHolder);
        mobileHolder = findViewById(R.id.textViewMobileHolder);
        specialtyHolder = findViewById(R.id.textViewSpecialtyHolder);
        usernameHolder = findViewById(R.id.textViewUsernameHolder);
//        passwordHolder = findViewById(R.id.textViewPasswordHolder);
        doctorIdHolder = findViewById(R.id.textViewDoctorIDHolder);
        showInfoLink = findViewById(R.id.textViewShowInfo);
        backBtn = findViewById(R.id.buttonBack);
        termsBtn = findViewById(R.id.buttonTerms);
        logoutBtn = findViewById(R.id.buttonLogout);
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
            Intent intent = new Intent(DoctorProfileActivity.this, SignInPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showPasswordDialog() {
        // 1. Create the new Bottom Sheet Fragment
        com.example.sttherese.patient.activities.PasswordEntryBottomSheet bottomSheet = new com.example.sttherese.patient.activities.PasswordEntryBottomSheet();

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
        if (doctorIdHolder != null) doctorIdHolder.setText("*****");
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

        // 1. Fetch from 'users' collection using the Auth UID as the document ID (This is fine)
        db.collection("users").document(authUid).get().addOnSuccessListener(userSnap -> {
            if (userSnap.exists()) {
                String email = userSnap.getString("email");
                String roleType = userSnap.getString("user_role_type");
                emailHolder.setText(email != null ? email : "N/A");
                emailCard.setText(email != null ? email : "N/A");

                // 2. Fetch the 'patients' document by querying the 'userId' field
                // You are searching for the document where the field "userId" equals the Auth UID
                db.collection("doctors")
                        .whereEqualTo("user_id", authUid) // Find the patient document linked to this Auth UID
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                // Found the matching patient document
                                com.google.firebase.firestore.DocumentSnapshot patientSnap = querySnapshot.getDocuments().get(0);

                                String doctorId = patientSnap.getId();

//                                String firstName = capitalizeWords(patientSnap.getString("first_name"));
//                                String middleName = patientSnap.getString("middle_name") != null ?
//                                        capitalizeWords(patientSnap.getString("middle_name")) + " " : "";
//                                String lastName = capitalizeWords(patientSnap.getString("last_name"));
//                                String fullName = firstName + " " + middleName + lastName;

                                nameHolder.setText(patientSnap.getString("name"));
                                nameCard.setText(patientSnap.getString("name"));

                                genderHolder.setText(capitalizeFirst(patientSnap.getString("gender")));

                                String dob = patientSnap.getString("dob");
                                dobHolder.setText(formatDate(dob));

                                mobileHolder.setText(patientSnap.getString("contact"));
                                specialtyHolder.setText(capitalizeWords(patientSnap.getString("specialty")));
                                specialtyCard.setText(capitalizeWords(patientSnap.getString("specialty")));

                                if (isInfoVisible) {
                                    usernameHolder.setText(email != null ? email.split("@")[0] : "");
                                    if (doctorIdHolder != null) {
                                        doctorIdHolder.setText(doctorId);
                                    }
                                } else {
                                    hideAccountInfo();
                                }
                            } else {
                                Toast.makeText(this, "Doctor details not found.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error querying doctor details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "User account not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(dateString);

            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return dateString;
        }
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
