package com.example.sttherese;

import com.example.sttherese.patient.activities.Home;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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
import androidx.appcompat.app.AlertDialog;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SignInPage extends AppCompatActivity {

    EditText editTextEmail, editTextPassword;
    Button buttonSignIn;
    TextView signUpText, forgotPassword;
    ImageView iconFacebook, iconGoogle;

    boolean passwordVisible = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_page);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        signUpText = findViewById(R.id.textSignUp);
        forgotPassword = findViewById(R.id.textForgotPassword);

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
        signUpText.setOnClickListener(v -> startActivity(new Intent(SignInPage.this, FindRecord.class)));
        forgotPassword.setOnClickListener(v -> startActivity(new Intent(SignInPage.this, FP_FindAccount.class)));
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
        editTextEmail.setError(null);
        editTextPassword.setError(null);

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

        buttonSignIn.setEnabled(false);
        checkLockoutAndSignIn(email, password);
    }

    private void checkLockoutAndSignIn(String email, String password) {
        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    DocumentSnapshot userSnapshot = querySnapshot != null && !querySnapshot.isEmpty()
                            ? querySnapshot.getDocuments().get(0)
                            : null;

                    if (isAccountLocked(userSnapshot)) {
                        buttonSignIn.setEnabled(true);
                        showCustomDialog(R.drawable.ic_acc_failed,
                                "Account temporarily locked due to too many failed attempts.",
                                buildLockedMessage(userSnapshot));
                        return;
                    }

                    signIn(email, password, userSnapshot);
                })
                .addOnFailureListener(e -> {
                    signIn(email, password, null);
                });
    }

    private void signIn(String email, String password, DocumentSnapshot userSnapshot) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    buttonSignIn.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            resetLoginAttempts(user.getUid(), email, userSnapshot);
                            fetchUserRoleAndRedirect(user.getUid(), email);
                        }
                    } else {
                        handleLoginFailure(email, userSnapshot, task.getException());
                    }
                });
    }

    private void handleLoginFailure(String email, DocumentSnapshot userSnapshot, Exception exception) {
        String errorCode = "";
        if (exception instanceof FirebaseAuthException) {
            errorCode = ((FirebaseAuthException) exception).getErrorCode();
        }

        if ("ERROR_USER_NOT_FOUND".equals(errorCode) || "ERROR_INVALID_EMAIL".equals(errorCode)) {
            showCustomDialog(R.drawable.ic_error, "Account Not Found", "No account found with this email address");
            return;
        }

        if (exception != null && exception.getMessage() != null
                && exception.getMessage().toLowerCase().contains("network")) {
            showCustomDialog(R.drawable.ic_error, "Network Error", "Please check your internet connection");
            return;
        }

        recordFailedLogin(email, userSnapshot);
    }

    private boolean isAccountLocked(DocumentSnapshot userSnapshot) {
        if (userSnapshot == null || !userSnapshot.exists()) return false;
        Date lockedUntil = userSnapshot.getDate("login_locked_until");
        if (lockedUntil == null) return false;

        if (lockedUntil.after(new Date())) {
            return true;
        }

        userSnapshot.getReference().update(
                "failed_login_attempts", 0,
                "login_locked_until", null
        );
        return false;
    }

    private void recordFailedLogin(String email, DocumentSnapshot userSnapshot) {
        if (userSnapshot == null || !userSnapshot.exists()) {
            showCustomDialog(R.drawable.ic_error, "Incorrect Email or Password", "Please check your email and password.");
            return;
        }

        Long attemptsLong = userSnapshot.getLong("failed_login_attempts");
        int attempts = attemptsLong == null ? 0 : attemptsLong.intValue();
        int nextAttempts = attempts + 1;

        Map<String, Object> updates = new HashMap<>();
        updates.put("failed_login_attempts", nextAttempts);
        updates.put("last_failed_login_at", FieldValue.serverTimestamp());

        if (nextAttempts >= MAX_LOGIN_ATTEMPTS) {
            Date lockedUntil = new Date(System.currentTimeMillis() + LOCKOUT_DURATION_MS);
            updates.put("login_locked_until", lockedUntil);
            userSnapshot.getReference().update(updates);
            showCustomDialog(R.drawable.ic_acc_lock, "Too many failed attempts!", "Your account has been locked for 15 minutes.");
        } else {
            userSnapshot.getReference().update(updates);
            showCustomDialog(R.drawable.ic_error, "Incorrect Email or Password", "Login Attempts: " + nextAttempts + "/5");
        }
    }

    private String buildLockedMessage(DocumentSnapshot userSnapshot) {
        Date lockedUntil = userSnapshot != null ? userSnapshot.getDate("login_locked_until") : null;
        if (lockedUntil == null) return "Account is temporarily locked. Please try again later.";
        long remainingMillis = Math.max(0, lockedUntil.getTime() - System.currentTimeMillis());
        long remainingMinutes = Math.max(1, (long) Math.ceil(remainingMillis / 60000.0));
        return "Try again in " + remainingMinutes + " minute(s).";
    }

    private String formatLockoutTime(Date date) {
        return new java.text.SimpleDateFormat("h:mm a, dd MMMM yyyy", java.util.Locale.getDefault()).format(date);
    }

    private void showLoginStatusDialog(int iconResId, String title, String message) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_status, null);

        ImageView icon = dialogView.findViewById(R.id.dialog_icon);
        TextView titleText = dialogView.findViewById(R.id.dialog_title);
        TextView msg = dialogView.findViewById(R.id.dialog_message);
        Button closeBtn = dialogView.findViewById(R.id.dialog_close_btn);

        icon.setImageResource(iconResId);
        titleText.setText(title);
        msg.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setView(dialogView)
                .create();
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showCustomDialog(int iconResId, String title, String message) {
        showLoginStatusDialog(iconResId, title, message);
    }

    private void resetLoginAttempts(String uid, String email, DocumentSnapshot userSnapshot) {
        DocumentReference ref = userSnapshot != null && userSnapshot.exists()
                ? userSnapshot.getReference()
                : db.collection("users").document(uid);
        ref.update(
                "failed_login_attempts", 0,
                "login_locked_until", null,
                "last_login_at", FieldValue.serverTimestamp()
        );
    }

    private void fetchUserRoleAndRedirect(String userId, String email) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("deleted_at") && documentSnapshot.get("deleted_at") != null) {
                            mAuth.signOut();
                            showCustomDialog(R.drawable.ic_error, "Account Archived", "This account has been archived. Please contact the clinic.");
                            return;
                        }

                        String role = documentSnapshot.getString("user_role_type");
                        if (role == null) role = "patient";
                        role = role.toLowerCase();

                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("user_doc_id", userId);
                        editor.putString("email", email);
                        editor.putString("user_role_type", role);
                        editor.apply();

                        if ("doctor".equals(role)) {
                            lookupDoctorIdAndRedirect(userId);
                        } else {
                            lookupPatientIdAndRedirect(userId);
                        }
                    }
                });
    }

    private void lookupDoctorIdAndRedirect(String authUid) {
        db.collection("doctors").whereEqualTo("user_id", authUid).limit(1).get()
                .addOnSuccessListener(querySnapshots -> {
                    if (!querySnapshots.isEmpty()) {
                        String docId = querySnapshots.getDocuments().get(0).getId();
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                .putString("doctor_doc_id", docId).apply();
                    }
                    startActivity(new Intent(SignInPage.this, DoctorHomeActivity.class));
                    finish();
                });
    }

    private void lookupPatientIdAndRedirect(String authUid) {
        db.collection("patients").whereEqualTo("userId", authUid).limit(1).get()
                .addOnSuccessListener(querySnapshots -> {
                    if (!querySnapshots.isEmpty()) {
                        handlePatientProfileForLogin(querySnapshots.getDocuments().get(0));
                    } else {
                        db.collection("patients").whereEqualTo("user_id", authUid).limit(1).get()
                                .addOnSuccessListener(secondQuery -> {
                                    if (!secondQuery.isEmpty()) {
                                        handlePatientProfileForLogin(secondQuery.getDocuments().get(0));
                                    } else {
                                        startActivity(new Intent(SignInPage.this, Home.class));
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    startActivity(new Intent(SignInPage.this, Home.class));
                                    finish();
                                });
                    }
                });
    }

    private void handlePatientProfileForLogin(DocumentSnapshot patientSnapshot) {
        Boolean archived = patientSnapshot.getBoolean("archived");
        Boolean active = patientSnapshot.getBoolean("is_active");

        if (Boolean.TRUE.equals(archived) || Boolean.FALSE.equals(active)) {
            mAuth.signOut();
            showCustomDialog(R.drawable.ic_error, "Account Archived", "This patient account has been archived. Please contact the clinic.");
            return;
        }

        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                .putString("patient_firestore_id", patientSnapshot.getId())
                .apply();
        startActivity(new Intent(SignInPage.this, Home.class));
        finish();
    }
}
