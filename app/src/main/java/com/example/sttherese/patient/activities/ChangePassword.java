package com.example.sttherese.patient.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.method.PasswordTransformationMethod;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sttherese.R;
import com.example.sttherese.SignInPage;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChangePassword extends AppCompatActivity {

    private EditText editTextNewPassword, editTextConfirmPassword;
    private TextView textPasswordMatch, passwordStrengthText;
    private Button buttonReset;
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userEmail;
    private boolean isVerified;
    private String verificationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        passwordStrengthText = findViewById(R.id.passwordStrength);
        textPasswordMatch = findViewById(R.id.textPasswordMatch);
        buttonReset = findViewById(R.id.buttonReset);

        userEmail = getIntent().getStringExtra("email");
        isVerified = getIntent().getBooleanExtra("verified", false);
        verificationCode = getIntent().getStringExtra("code");

        if (userEmail == null || !isVerified || verificationCode == null) {
            Toast.makeText(this, "Unauthorized access or missing verification.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupPasswordVisibilityToggle();
        editTextNewPassword.addTextChangedListener(passwordStrengthWatcher);
        editTextConfirmPassword.addTextChangedListener(confirmPasswordWatcher);

        buttonReset.setOnClickListener(v -> {
            String newPass = editTextNewPassword.getText().toString().trim();
            String confirmPass = editTextConfirmPassword.getText().toString().trim();

            if (newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (getPasswordStrength(newPass) < 1) {
                Toast.makeText(this, "Password is too weak. Use at least 6 characters",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            buttonReset.setEnabled(false);
            buttonReset.setText("Updating password...");

            updatePasswordWithVerification(newPass);
        });
    }

    private void setupPasswordVisibilityToggle() {
        editTextNewPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editTextNewPassword.getRight()
                        - editTextNewPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility(editTextNewPassword, true);
                    return true;
                }
            }
            return false;
        });

        editTextConfirmPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editTextConfirmPassword.getRight()
                        - editTextConfirmPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility(editTextConfirmPassword, false);
                    return true;
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
            String password = editTextNewPassword.getText().toString();
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

        if (score <= 1) return 0;
        else if (score == 2) return 1;
        else return 2;
    }

    private void updatePasswordWithVerification(String newPassword) {
        AuthCredential credential = EmailAuthProvider.getCredential(userEmail, verificationCode);

        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();

                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid -> {
                                // Password updated successfully, now cleanup
                                cleanupAndShowSuccess();
                            })
                            .addOnFailureListener(e -> {
                                resetButton();
                                showCustomDialog(
                                        R.drawable.ic_error,
                                        "Password Update Failed",
                                        "Error updating password: " + e.getMessage(),
                                        false
                                );
                            });
                })
                .addOnFailureListener(e -> {
                    resetButton();
                    showCustomDialog(
                            R.drawable.ic_error,
                            "Verification Failed",
                            "Could not authenticate with the code. Please try again. Error: " + e.getMessage(),
                            false
                    );
                });
    }

    private void cleanupAndShowSuccess() {
        String docId = userEmail.replace(".", "_").replace("@", "_at_");
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("password_resets")
                .child(docId);

        // Set a timeout for the cleanup operation
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            // If cleanup takes too long (>10 seconds), show success anyway
            resetButton();
            showCustomDialog(
                    R.drawable.ic_check,
                    "Success!",
                    "Your password has been updated successfully!",
                    true
            );
        };

        // Start 10-second timeout
        timeoutHandler.postDelayed(timeoutRunnable, 10000);

        ref.removeValue()
                .addOnCompleteListener(task -> {
                    // Cancel the timeout since operation completed
                    timeoutHandler.removeCallbacks(timeoutRunnable);

                    // Reset button state
                    resetButton();

                    if (task.isSuccessful()) {
                        showCustomDialog(
                                R.drawable.ic_check,
                                "Success!",
                                "Your password has been updated successfully!",
                                true
                        );
                    } else {
                        // Cleanup failed, but password was changed
                        showCustomDialog(
                                R.drawable.ic_check,
                                "Success!",
                                "Your password has been updated successfully!",
                                true
                        );
                    }
                });
    }

    private void resetButton() {
        buttonReset.setEnabled(true);
        buttonReset.setText("Reset Password");
    }

    private void showCustomDialog(int iconResId, String title, String message, boolean isSuccess) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_status, null);

        ImageView icon = dialogView.findViewById(R.id.dialog_icon);
        TextView titleText = dialogView.findViewById(R.id.dialog_title);
        TextView msg = dialogView.findViewById(R.id.dialog_message);
        Button closeBtn = dialogView.findViewById(R.id.dialog_close_btn);

        icon.setImageResource(iconResId);
        titleText.setText(title);
        msg.setText(message);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(
                this, R.style.CustomAlertDialog)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        closeBtn.setOnClickListener(v -> {
            dialog.dismiss();
            if (isSuccess) {
                Intent intent = new Intent(ChangePassword.this, SignInPage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        dialog.show();
    }
}