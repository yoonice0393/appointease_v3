package com.example.sttherese;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
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
            editTextPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_slash, 0);
        } else {
            editTextPassword.setTransformationMethod(new android.text.method.PasswordTransformationMethod());
            editTextPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
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

                                // Optional: Fetch additional user data from Firestore
                                fetchUserDataFromFirestore(userDocId);

                                Toast.makeText(SignInPage.this, "Login successful!", Toast.LENGTH_SHORT).show();

                                // Navigate to Home
                                Intent intent = new Intent(SignInPage.this, Home.class);
                                intent.putExtra(USER_DOC_ID_EXTRA, userDocId);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            // Sign in failed - record failed attempt
                            recordFailedAttempt(email);

                            String errorMessage = "Authentication failed. Check your email and password.";
                            if (task.getException() != null) {
                                String exception = task.getException().getMessage();
                                if (exception != null) {
                                    if (exception.contains("no user record")) {
                                        errorMessage = "No account found with this email";
                                    } else if (exception.contains("password is invalid")) {
                                        errorMessage = "Incorrect password";
                                    } else if (exception.contains("network")) {
                                        errorMessage = "Network error. Please check your connection";
                                    } else {
                                        errorMessage = exception;
                                    }
                                }
                            }
                            Toast.makeText(SignInPage.this, errorMessage, Toast.LENGTH_LONG).show();
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
                                    Toast.makeText(this,
                                            "⚠️ Account temporarily locked due to too many failed attempts.\n" +
                                                    "Try again in " + remainingMinutes + " minute(s).",
                                            Toast.LENGTH_LONG).show();
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
                                    Toast.makeText(this,
                                            "❌ Too many failed attempts!\n" +
                                                    "Your account has been locked for 15 minutes.",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this,
                                            "⚠️ Login failed. Attempts: " + finalCurrentAttempts + "/5",
                                            Toast.LENGTH_SHORT).show();
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
    private void fetchUserDataFromFirestore(String userId) {
        db.collection("patients").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("first_name");
                        String lastName = documentSnapshot.getString("last_name");
                        String userRoleType = documentSnapshot.getString("user_role_type");

                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        if (firstName != null) editor.putString("first_name", firstName);
                        if (lastName != null) editor.putString("last_name", lastName);
                        if (userRoleType != null) editor.putString("user_role_type", userRoleType);
                        editor.apply();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FIRESTORE", "Error fetching user data: " + e.getMessage());
                });
    }
}