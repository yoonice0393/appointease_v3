package com.example.sttherese;

import com.example.sttherese.patient.activities.Home;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.sttherese.doctor.DoctorHomeActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;

import java.util.HashMap;
import java.util.Map;

public class SignInPage extends AppCompatActivity {

    EditText editTextEmail, editTextPassword;
    Button buttonSignIn;
    TextView signUpText, forgotPassword;
    ImageView iconFacebook, iconGoogle;

    boolean passwordVisible = false;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String USER_DOC_ID_EXTRA = "USER_DOC_ID_EXTRA";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_page);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        signUpText = findViewById(R.id.textSignUp);
        forgotPassword = findViewById(R.id.textForgotPassword);

        // Toggle password visibility when tapping the eye icon
        editTextPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable[] drawables = editTextPassword.getCompoundDrawables();
                if (drawables[2] != null) {
                    int drawableWidth = drawables[2].getBounds().width();
                    if (event.getRawX() >= (editTextPassword.getRight() - drawableWidth - editTextPassword.getPaddingEnd())) {
                        togglePasswordVisibility();
                        return true;
                    }
                }
            }
            return false;
        });

        buttonSignIn.setOnClickListener(v -> Login());

        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(SignInPage.this, FindRecord.class);
            startActivity(intent);
        });

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(SignInPage.this, FP_FindAccount.class);
            startActivity(intent);
        });

        // Real-time email validation
        editTextEmail.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String emailInput = s.toString().trim();
                if (emailInput.isEmpty()) {
                    editTextEmail.setError("Email is required");
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                    editTextEmail.setError("Invalid email address");
                } else {
                    editTextEmail.setError(null);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            editTextPassword.setTransformationMethod(null);
            editTextPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
        } else {
            editTextPassword.setTransformationMethod(new android.text.method.PasswordTransformationMethod());
            editTextPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_slash, 0);
        }

        editTextPassword.setSelection(editTextPassword.getText().length());
    }

    private void Login() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        boolean hasError = false;

        // Reset previous errors
        editTextEmail.setError(null);
        editTextPassword.setError(null);

        // Empty field validation
        if (email.isEmpty()) {
            editTextEmail.setError("Email is required");
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Invalid email address");
            hasError = true;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            hasError = true;
        }

        if (hasError) {
            Toast.makeText(this, "Please correct the highlighted fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple submissions
        buttonSignIn.setEnabled(false);

        // Check if account is temporarily locked
        checkLoginAttempts(email, canProceed -> {
            if (!canProceed) {
                buttonSignIn.setEnabled(true);
                return; // Account is locked
            }

            // Firebase Authentication Sign In
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        buttonSignIn.setEnabled(true);

                        if (task.isSuccessful()) {
                            // Sign in success - clear login attempts
                            clearLoginAttempts(email);

                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();
                                String userDocId = userId;

                                // Save to SharedPreferences
                                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("user_doc_id", userDocId);
                                editor.putString("email", email);
                                editor.apply();

                                fetchUserRoleAndRedirect(userDocId);
                                Toast.makeText(SignInPage.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Sign in failed - check the error code
                            if (task.getException() != null) {
                                Exception exception = task.getException();
                                String errorCode = "";

                                // Try to get the error code from FirebaseAuthException
                                if (exception instanceof com.google.firebase.auth.FirebaseAuthException) {
                                    errorCode = ((com.google.firebase.auth.FirebaseAuthException) exception).getErrorCode();
                                    android.util.Log.e("LOGIN_ERROR", "Firebase Error Code: " + errorCode);
                                }

                                android.util.Log.e("LOGIN_ERROR", "Exception Class: " + exception.getClass().getName());
                                android.util.Log.e("LOGIN_ERROR", "Exception Message: " + exception.getMessage());

                                // Check error code for user not found
                                if ("ERROR_USER_NOT_FOUND".equals(errorCode) ||
                                        "ERROR_INVALID_EMAIL".equals(errorCode)) {

                                    android.util.Log.w("LOGIN_ERROR", ">>> USER NOT FOUND - NOT RECORDING ATTEMPT <<<");
                                    showCustomDialog(R.drawable.ic_error, "Account Not Found",
                                            "No account found with this email address");

                                }
                                // Check error code for wrong password
                                else if ("ERROR_WRONG_PASSWORD".equals(errorCode) ||
                                        "ERROR_INVALID_CREDENTIAL".equals(errorCode)) {

                                    android.util.Log.w("LOGIN_ERROR", ">>> WRONG PASSWORD - RECORDING ATTEMPT <<<");
                                    recordFailedAttempt(email);

                                }
                                // Network error
                                else if (exception.getMessage() != null &&
                                        exception.getMessage().toLowerCase().contains("network")) {

                                    android.util.Log.w("LOGIN_ERROR", ">>> NETWORK ERROR - NOT RECORDING ATTEMPT <<<");
                                    showCustomDialog(R.drawable.ic_error, "Network Error",
                                            "Please check your internet connection");

                                }
                                // Fallback: If we can't determine the error, assume wrong password to be safe
                                else {
                                    android.util.Log.w("LOGIN_ERROR", ">>> UNKNOWN ERROR - RECORDING ATTEMPT (to be safe) <<<");
                                    recordFailedAttempt(email);
                                }

                            } else {
                                android.util.Log.e("LOGIN_ERROR", ">>> NO EXCEPTION OBJECT <<<");
                                showCustomDialog(R.drawable.ic_error, "Login Failed",
                                        "Authentication failed. Please try again.");
                            }
                        }
                    });
        });
    }
    /**
     * Check if user has exceeded login attempts (5 attempts = 15 minute lockout)
     */
    private void checkLoginAttempts(String email, LoginAttemptsCallback callback) {
        String docId = email.replace(".", "_").replace("@", "_at_");

        db.collection("login_attempts").document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long failedAttempts = documentSnapshot.getLong("failed_attempts");
                        com.google.firebase.Timestamp attemptTime = documentSnapshot.getTimestamp("attempt_time");

                        if (failedAttempts != null && failedAttempts >= 5) {
                            // Check if 15 minutes have passed
                            if (attemptTime != null) {
                                long timeDiff = System.currentTimeMillis() - attemptTime.toDate().getTime();
                                long minutesPassed = timeDiff / (60 * 1000);

                                if (minutesPassed < 15) {
                                    long remainingMinutes = 15 - minutesPassed;
                                    showCustomDialog(R.drawable.ic_acc_failed,"Account temporarily locked due to too many failed attempts.", "Try again in " + remainingMinutes + " minute(s).");

                                    callback.onResult(false);
                                    return;
                                } else {
                                    // Lock period expired - clear attempts and allow login
                                    clearLoginAttempts(email);
                                }
                            }
                        }
                    }
                    // Either no document exists, or attempts < 5, or lock expired
                    callback.onResult(true);
                })
                .addOnFailureListener(e -> {
                    // If error checking attempts, allow login attempt
                    android.util.Log.e("LOGIN_ATTEMPTS", "Error checking attempts: " + e.getMessage());
                    callback.onResult(true);
                });
    }

    /**
     * Record a failed login attempt
     */
    private void recordFailedAttempt(String email) {
        String docId = email.replace(".", "_").replace("@", "_at_");

        db.collection("login_attempts").document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    long currentAttempts = 0;

                    if (documentSnapshot.exists()) {
                        Long attempts = documentSnapshot.getLong("failed_attempts");
                        currentAttempts = (attempts != null) ? attempts : 0;
                    }

                    currentAttempts++;

                    Map<String, Object> attemptData = new HashMap<>();
                    attemptData.put("email", email);
                    attemptData.put("failed_attempts", currentAttempts);
                    attemptData.put("attempt_time", com.google.firebase.firestore.FieldValue.serverTimestamp());

                    long finalCurrentAttempts = currentAttempts;
                    db.collection("login_attempts").document(docId)
                            .set(attemptData)
                            .addOnSuccessListener(aVoid -> {
                                if (finalCurrentAttempts >= 5) {
                                    showCustomDialog(R.drawable.ic_acc_lock,"Too many failed attempts!", "Your account has been locked for 15 minutes.");

                                } else {
                                    showCustomDialog(R.drawable.ic_error,"Incorrect Email or Password", "Login Attempts: " + finalCurrentAttempts + "/5");
                                }
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("LOGIN_ATTEMPTS", "Error recording attempt: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LOGIN_ATTEMPTS", "Error checking attempts: " + e.getMessage());
                });
    }

    /**
     * Clear login attempts after successful login
     */
    private void clearLoginAttempts(String email) {
        String docId = email.replace(".", "_").replace("@", "_at_");
        db.collection("login_attempts").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("LOGIN_ATTEMPTS", "Login attempts cleared for: " + email);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LOGIN_ATTEMPTS", "Error clearing attempts: " + e.getMessage());
                });
    }

    /**
     * Callback interface for async login attempts check
     */
    interface LoginAttemptsCallback {
        void onResult(boolean canProceed);
    }

    /**
     * Fetch additional user data from Firestore
     */
    private void fetchUserRoleAndRedirect(String userId) {

        // Use a tag for easy filtering in Logcat
        final String REDIRECT_TAG = "ROLE_REDIRECT";
        android.util.Log.d(REDIRECT_TAG, "Attempting to fetch role for Auth UID: " + userId);

        // Query the 'users' collection where the role is stored.
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userRoleType = "patient"; // Default role if any issue occurs

                    if (documentSnapshot.exists()) {
                        android.util.Log.d(REDIRECT_TAG, "User document EXISTS in 'users' collection.");

                        // Fetch the role field
                        String fetchedRole = documentSnapshot.getString("user_role_type");

                        // Check if the role field exists and is not empty
                        if (fetchedRole != null && !fetchedRole.isEmpty()) {
                            userRoleType = fetchedRole.toLowerCase(); // Standardize to lowercase for safety
                            android.util.Log.d(REDIRECT_TAG, "Fetched Role: " + userRoleType);
                        } else {
                            android.util.Log.w(REDIRECT_TAG, "user_role_type field is missing or null. Defaulting to patient.");
                        }

                        // Save the role type to SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("user_role_type", userRoleType);
                        editor.apply();

                    } else {
                        android.util.Log.e(REDIRECT_TAG, "User document DOES NOT EXIST in 'users' collection for this Auth UID.");
                        // Keep default role as "patient"
                    }

                    // Determine Navigation
                    Intent intent;

                    // CRITICAL LOGIC: Compare the final, sanitized role
                    if ("doctor".equals(userRoleType)) {
                        intent = new Intent(SignInPage.this, DoctorHomeActivity.class);
                        android.util.Log.i(REDIRECT_TAG, "Redirecting to DOCTOR HOME.");
                    } else {
                        intent = new Intent(SignInPage.this, Home.class);
                        android.util.Log.i(REDIRECT_TAG, "Redirecting to PATIENT HOME (Role: " + userRoleType + ").");
                    }

                    // Start Activity and Finish Sign-In
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(REDIRECT_TAG, "FIRESTORE READ FAILED: " + e.getMessage());
                    // Fallback: If Firestore fails entirely, assume patient
                    Intent fallbackIntent = new Intent(SignInPage.this, Home.class);
                    fallbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(fallbackIntent);
                    finish();
                });
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

        });

        dialog.show();
    }
}