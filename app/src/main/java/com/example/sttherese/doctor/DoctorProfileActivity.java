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
import com.example.sttherese.patient.activities.ProfileActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DoctorProfileActivity extends AppCompatActivity {



    ImageView backBtn;
    MaterialButton logoutBtn;
    TextView dialogTitle, nameHolder, genderHolder, dobHolder, emailHolder, mobileHolder,
            specialtyHolder, usernameHolder, passwordHolder, userIdHolder, nameCard, emailCard, specialtyCard;
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

    }

    private void initializeView() {
        nameHolder = findViewById(R.id.textViewNameHolder);
        nameCard = findViewById(R.id.textViewNameCard);
        emailCard = findViewById(R.id.textViewEmailCard);
        specialtyCard = findViewById(R.id.textViewSpecialty);
        genderHolder = findViewById(R.id.textViewGenderHolder);
        dobHolder = findViewById(R.id.textViewDOBHolder);
        emailHolder = findViewById(R.id.textViewEmailHolder);
        mobileHolder = findViewById(R.id.textViewMobileHolder);
        specialtyHolder = findViewById(R.id.textViewSpecialtyHolder);
        usernameHolder = findViewById(R.id.textViewUsernameHolder);
        passwordHolder = findViewById(R.id.textViewPasswordHolder);
        userIdHolder = findViewById(R.id.textViewUserIDHolder);
        showInfoLink = findViewById(R.id.textViewShowInfo);
        backBtn = findViewById(R.id.buttonBack);
        logoutBtn = findViewById(R.id.buttonLogout);
    } private void showLogoutDialog() {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_password_verification, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText passwordInput = dialogView.findViewById(R.id.editTextPassword);
        ImageView togglePassword = dialogView.findViewById(R.id.imageViewTogglePassword);
        MaterialButton proceedBtn = dialogView.findViewById(R.id.buttonProceed);

        togglePassword.setOnClickListener(v -> {
            if (passwordInput.getTransformationMethod() instanceof PasswordTransformationMethod) {
                passwordInput.setTransformationMethod(null);
                togglePassword.setImageResource(R.drawable.ic_eye);
            } else {
                passwordInput.setTransformationMethod(new PasswordTransformationMethod());
                togglePassword.setImageResource(R.drawable.ic_eye_slash);
            }
            passwordInput.setSelection(passwordInput.getText().length());
        });

        proceedBtn.setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            } else {
                verifyPassword(password, dialog);
            }
        });

        dialog.show();
    }

    private void verifyPassword(String password, AlertDialog dialog) {
        // 1. Get current user and their email
        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            dialog.dismiss();
            return;
        }

        String email = user.getEmail();

        // 2. Use Firebase Auth to re-authenticate the user
        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dialog.dismiss();
                        showAccountInfo();
                        Toast.makeText(this, "Access granted", Toast.LENGTH_SHORT).show();
                    } else {
                        // This handles incorrect passwords
                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                        android.util.Log.e("Auth", "Re-authentication failed: " + task.getException().getMessage());
                    }
                });
    }

    private void hideAccountInfo() {
        usernameHolder.setText("******");
        passwordHolder.setText("**********");
        if (userIdHolder != null) userIdHolder.setText("*****");
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
                        .whereEqualTo("userId", authUid) // Find the patient document linked to this Auth UID
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                // Found the matching patient document
                                com.google.firebase.firestore.DocumentSnapshot patientSnap = querySnapshot.getDocuments().get(0);


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
                                    // NOTE: The 'password' field is not typically stored in Firestore
                                    // You should remove this line if it's not needed:
                                    passwordHolder.setText(userSnap.getString("password"));
                                    if (userIdHolder != null) userIdHolder.setText(authUid);
                                } else {
                                    hideAccountInfo();
                                }
                            } else {
                                Toast.makeText(this, "Doctor details not found (Query failed).", Toast.LENGTH_SHORT).show();
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
