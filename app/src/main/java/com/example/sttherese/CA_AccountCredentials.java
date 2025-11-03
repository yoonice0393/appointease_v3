package com.example.sttherese;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CA_AccountCredentials extends AppCompatActivity {

    EditText editTextEmail, editTextPassword, editTextConfirmPassword;
    TextView passwordStrengthText, textPasswordMatch;
    CheckBox checkBoxTerms;
    Button buttonSignUp;
    ImageView backBtn;

    // Data from previous screen
    String firstName, middleName, lastName, dob, gender, contact, address;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // Password visibility
    boolean passwordVisible = false;
    boolean confirmPasswordVisible = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ca_account_credentials);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        passwordStrengthText = findViewById(R.id.passwordStrength);
        textPasswordMatch = findViewById(R.id.textPasswordMatch);
//        checkBoxTerms = findViewById(R.id.checkBoxTerms);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        backBtn = findViewById(R.id.buttonBack);

        // Get data from previous screen
        Intent intent = getIntent();
        firstName = intent.getStringExtra("first_name");
        middleName = intent.getStringExtra("middle_name");
        lastName = intent.getStringExtra("last_name");
        dob = intent.getStringExtra("dob");
        gender = intent.getStringExtra("gender");
        contact = intent.getStringExtra("contact");
        address = intent.getStringExtra("address");

        // Toggle password visibility
        setupPasswordVisibilityToggle();

        // Add password strength checker
        editTextPassword.addTextChangedListener(passwordStrengthWatcher);

        // Add password match checker
        editTextConfirmPassword.addTextChangedListener(confirmPasswordWatcher);

        // Email validation
        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String emailInput = s.toString().trim();
                if (!emailInput.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                    editTextEmail.setError("Invalid email address");
                } else {
                    editTextEmail.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        buttonSignUp.setOnClickListener(v -> validateAndSignUp());
        backBtn.setOnClickListener(v -> goBackToPersonalDetails());
    }

    private void setupPasswordVisibilityToggle() {
        editTextPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable[] drawables = editTextPassword.getCompoundDrawables();
                if (drawables[2] != null) {
                    int drawableWidth = drawables[2].getBounds().width();
                    if (event.getRawX() >= (editTextPassword.getRight() - drawableWidth - editTextPassword.getPaddingEnd())) {
                        togglePasswordVisibility(editTextPassword, true);
                        return true;
                    }
                }
            }
            return false;
        });

        editTextConfirmPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable[] drawables = editTextConfirmPassword.getCompoundDrawables();
                if (drawables[2] != null) {
                    int drawableWidth = drawables[2].getBounds().width();
                    if (event.getRawX() >= (editTextConfirmPassword.getRight() - drawableWidth - editTextConfirmPassword.getPaddingEnd())) {
                        togglePasswordVisibility(editTextConfirmPassword, false);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility(EditText editText, boolean isPasswordField) {
        if (isPasswordField) {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                editText.setTransformationMethod(null);
                editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_slash, 0);
            } else {
                editText.setTransformationMethod(new PasswordTransformationMethod());
                editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
            }
        } else {
            confirmPasswordVisible = !confirmPasswordVisible;
            if (confirmPasswordVisible) {
                editText.setTransformationMethod(null);
                editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_slash, 0);
            } else {
                editText.setTransformationMethod(new PasswordTransformationMethod());
                editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
            }
        }
        editText.setSelection(editText.getText().length());
    }

    private final TextWatcher passwordStrengthWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String password = s.toString();
            if (password.isEmpty()) {
                passwordStrengthText.setText("");
                return;
            }

            int strength = getPasswordStrength(password);

            switch (strength) {
                case 0:
                    passwordStrengthText.setText("Weak Password");
                    passwordStrengthText.setTextColor(Color.RED);
                    break;
                case 1:
                    passwordStrengthText.setText("Medium Password");
                    passwordStrengthText.setTextColor(Color.parseColor("#FFA500"));
                    break;
                case 2:
                    passwordStrengthText.setText("Strong Password");
                    passwordStrengthText.setTextColor(Color.GREEN);
                    break;
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    private final TextWatcher confirmPasswordWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String password = editTextPassword.getText().toString();
            String confirm = s.toString();

            if (confirm.isEmpty()) {
                textPasswordMatch.setVisibility(View.GONE);
            } else if (!password.equals(confirm)) {
                textPasswordMatch.setVisibility(View.VISIBLE);
                textPasswordMatch.setText("Passwords do not match");
                textPasswordMatch.setTextColor(Color.RED);
            } else {
                textPasswordMatch.setVisibility(View.VISIBLE);
                textPasswordMatch.setText("Passwords match");
                textPasswordMatch.setTextColor(Color.GREEN);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    private int getPasswordStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches("(?=.*[A-Z])(?=.*[a-z]).+")) score++;
        if (password.matches("(?=.*[0-9])(?=.*[!@#$%^&*(),.?\":{}|<>]).+")) score++;

        if (score <= 1) return 0; // Weak
        else if (score == 2) return 1; // Medium
        else return 2; // Strong
    }

    private void validateAndSignUp() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Reset errors
        editTextEmail.setError(null);
        editTextPassword.setError(null);
        editTextConfirmPassword.setError(null);

        boolean hasError = false;

        // Validate email
        if (email.isEmpty()) {
            editTextEmail.setError("Email is required");
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Invalid email address");
            hasError = true;
        }

        // Validate password
        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            hasError = true;
        } else if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            hasError = true;
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            editTextConfirmPassword.setError("Please confirm your password");
            hasError = true;
        } else if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match");
            hasError = true;
        }

        // Check terms and conditions
//        if (!checkBoxTerms.isChecked()) {
//            Toast.makeText(this, "Please agree to Terms and Conditions", Toast.LENGTH_SHORT).show();
//            hasError = true;
//        }

        if (hasError) {
            Toast.makeText(this, "Please correct the errors", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent double submission
        buttonSignUp.setEnabled(false);
        buttonSignUp.setText("Creating account...");

        // Create Firebase account
        createFirebaseAccount(email, password);
    }

    private void createFirebaseAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();


                            saveUserToFirestore(userId, email)
                                    .addOnSuccessListener(aVoid -> {
                                        savePatientRecord(userId)
                                                .addOnSuccessListener(docRef -> {
                                                    // SUCCESS: ALL DATA SAVED. NOW SEND CODE.
                                                    generateAndSendVerificationCode(email);
                                                })
                                                .addOnFailureListener(e -> handleSignUpFailure("Error saving patient data: " + e.getMessage()));
                                    })
                                    .addOnFailureListener(e -> handleSignUpFailure("Error saving user: " + e.getMessage()));
                            // END: CHAINED OPERATIONS
                        }
                    } else {
                        handleSignUpFailure(task.getException() != null ? task.getException().getMessage() : "Sign up failed");
                    }
                });
    }

    // 💡 New helper method to handle cleanup
    private void handleSignUpFailure(String errorMessage) {
        buttonSignUp.setEnabled(true);
        buttonSignUp.setText("Sign Up");
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        // Optional: Log the user out if they were successfully authenticated but data failed to save
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().delete(); // Prevents orphaned Firebase users
        }
    }

    private com.google.android.gms.tasks.Task<Void> saveUserToFirestore(String userId, String email) {
        // Create user document
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("user_role_id", "4"); // Patient role
        userData.put("user_role_type", "patient");
        userData.put("created_at", com.google.firebase.firestore.FieldValue.serverTimestamp());
        userData.put("updated_at", com.google.firebase.firestore.FieldValue.serverTimestamp());
        userData.put("deleted_at", null);

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User account created!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving user: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
        return db.collection("users").document(userId).set(userData);
    }

    private com.google.android.gms.tasks.Task<DocumentReference> savePatientRecord(String userId) {
        // Calculate age from DOB
        int age = calculateAge(dob);

        // Create patient document
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("userId", userId); // Reference to user account
        patientData.put("first_name", firstName);
        patientData.put("middle_name", middleName);
        patientData.put("last_name", lastName);
        patientData.put("dob", dob);
        patientData.put("age", age);
        patientData.put("gender", gender);
        patientData.put("contact", contact);
        patientData.put("address", address);
        patientData.put("created_at", com.google.firebase.firestore.FieldValue.serverTimestamp());
        patientData.put("updated_at", com.google.firebase.firestore.FieldValue.serverTimestamp());
        patientData.put("deleted_at", null);

        db.collection("patients").add(patientData)
                .addOnSuccessListener(documentReference -> {
                    // Success! Everything saved
//
//                    showCustomDialog(R.drawable.ic_check,"Account created successfully!","Redirecting to sign in...");
                })
                .addOnFailureListener(e -> {
                    buttonSignUp.setEnabled(true);
                    buttonSignUp.setText("Sign Up");
                    Toast.makeText(this, "Error saving patient data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });

        return db.collection("patients").add(patientData);
    }
    private void generateAndSendVerificationCode(String email) {
        String emailKey = email.replace(".", "_");
        String verificationCode = String.format("%06d", (int) (Math.random() * 999999));
        long expiryTime = System.currentTimeMillis() + (10 * 60 * 1000); // valid for 10 minutes

        Map<String, Object> codeData = new HashMap<>();
        codeData.put("code", verificationCode);
        codeData.put("email", email);
        codeData.put("expiresAt", expiryTime);
        codeData.put("verified", false);

        db.collection("email_verifications").document(emailKey)
                .set(codeData)
                .addOnSuccessListener(unused -> {
                    StringRequest request = new StringRequest(Request.Method.POST, "http://192.168.88.250/user_api/send_verification_code.php",
                            response -> {
                                // Log or show a message
                                Log.d("EMAIL", "Response: " + response);
                            },
                            error -> {
                                Log.e("EMAIL", "Error: " + error.getMessage());
                            }) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> params = new HashMap<>();
                            params.put("email", email);
                            params.put("first_name", "User");
                            params.put("code", verificationCode); // from the generated code
                            return params;
                        }
                    };

                    Volley.newRequestQueue(this).add(request);

                    showCustomDialog(R.drawable.ic_email,"Email sent!","Please check your email for the verification code.");


                    // 2. ✅ FIX: Delay navigation by 3 seconds so the user can read the message
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Intent intent = new Intent(CA_AccountCredentials.this, CA_Confirmation.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish(); // Finish the current activity AFTER the delay
                    }, 3000); // 3000 milliseconds = 3 seconds
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to generate code: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    private int calculateAge(String dateOfBirth) {
        try {
            String[] parts = dateOfBirth.split("-");
            int birthYear = Integer.parseInt(parts[0]);
            int birthMonth = Integer.parseInt(parts[1]);
            int birthDay = Integer.parseInt(parts[2]);

            Calendar today = Calendar.getInstance();
            int currentYear = today.get(Calendar.YEAR);
            int currentMonth = today.get(Calendar.MONTH) + 1; // Month is 0-indexed
            int currentDay = today.get(Calendar.DAY_OF_MONTH);

            int age = currentYear - birthYear;

            // Adjust if birthday hasn't occurred this year
            if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
                age--;
            }

            return age;
        } catch (Exception e) {
            return 0;
        }
    }

//    private void showSuccessDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        View view = getLayoutInflater().inflate(R.layout.dialog_success, null);
//
//        TextView dialogMessage = view.findViewById(R.id.dialogMessage);
//        if (dialogMessage != null) {
//            dialogMessage.setText("Account created successfully!\nRedirecting to sign in...");
//        }
//
//        builder.setView(view);
//        builder.setCancelable(false);
//
//        AlertDialog dialog = builder.create();
//        if (dialog.getWindow() != null) {
//            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
//        }
//
//        dialog.show();
//
//        // Auto-dismiss after 2 seconds and navigate to sign in
//        new android.os.Handler().postDelayed(() -> {
//            dialog.dismiss();
//
//            Intent intent = new Intent(CA_AccountCredentials.this, CA_Confirmation.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            startActivity(intent);
//            finish();
//        }, 2000);
//    }

    private void goBackToPersonalDetails() {
        // Go back and preserve data
        Intent intent = new Intent(CA_AccountCredentials.this, CA_PersonalDetails.class);
        intent.putExtra("first_name", firstName);
        intent.putExtra("middle_name", middleName);
        intent.putExtra("last_name", lastName);
        intent.putExtra("dob", dob);
        intent.putExtra("gender", gender);
        intent.putExtra("contact", contact);
        intent.putExtra("address", address);
        startActivity(intent);
        finish();
    }

    private void showCustomDialog(int iconResId, String title, String message) {
        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_status, null);

        ImageView icon = dialogView.findViewById(R.id.dialog_icon);
        TextView titleText = dialogView.findViewById(R.id.dialog_title);
        TextView msg = dialogView.findViewById(R.id.dialog_message);
        Button closeBtn = dialogView.findViewById(R.id.dialog_close_btn);


        icon.setImageResource(iconResId);
        titleText.setText(title);
        msg.setText(message);

        // Build dialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setView(dialogView)
                .create();

        // make corners show properly
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        closeBtn.setOnClickListener(v -> {
            dialog.dismiss();
//                // optional: focus email field
//            editTextEmail.requestFocus();
        });

        dialog.show();
    }

//    @Override
//    public void onBackPressed() {
//        goBackToPersonalDetails();
//    }
}