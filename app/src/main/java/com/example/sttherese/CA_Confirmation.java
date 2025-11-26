package com.example.sttherese;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CA_Confirmation extends AppCompatActivity {

    EditText d1, d2, d3, d4, d5, d6;
    MaterialButton buttonConfirm;
    TextView resendLink;
    String email;
    TextView textResendTimer;
    TextView textError;

    FirebaseFirestore db;
    CountDownTimer resendTimer;
    final long TIMER_DURATION_MS = 60000; // 60 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ca_confirmation);

        d1 = findViewById(R.id.codeDigit1);
        d2 = findViewById(R.id.codeDigit2);
        d3 = findViewById(R.id.codeDigit3);
        d4 = findViewById(R.id.codeDigit4);
        d5 = findViewById(R.id.codeDigit5);
        d6 = findViewById(R.id.codeDigit6);
        buttonConfirm = findViewById(R.id.buttonConfirm);
        textError = findViewById(R.id.textError);
        textError.setVisibility(View.INVISIBLE);
        textResendTimer = findViewById(R.id.textResendTimer);
        resendLink = findViewById(R.id.textResendCode);

        email = getIntent().getStringExtra("email");
        db = FirebaseFirestore.getInstance();

        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Email not found. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupCodeInputs();
        startResendTimer();

        buttonConfirm.setOnClickListener(v -> verifyCode());
        resendLink.setOnClickListener(v -> resendCode());
    }

    private void setupCodeInputs() {
        EditText[] digits = {d1, d2, d3, d4, d5, d6};
        for (int i = 0; i < digits.length; i++) {
            final int index = i;
            digits[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < 5) digits[index + 1].requestFocus();
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            digits[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (digits[index].getText().toString().isEmpty() && index > 0) {
                        digits[index - 1].requestFocus();
                        digits[index - 1].setText("");
                    }
                }
                return false;
            });
        }
    }

    private void verifyCode() {
        String code = d1.getText().toString().trim() +
                d2.getText().toString().trim() +
                d3.getText().toString().trim() +
                d4.getText().toString().trim() +
                d5.getText().toString().trim() +
                d6.getText().toString().trim();

        if (code.length() != 6) {
            Toast.makeText(this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonConfirm.setEnabled(false);
//        buttonConfirm.setText("Verifying...");
        textError.setVisibility(View.GONE);

        String emailKey = email.replace(".", "_"); // use safe Firestore ID
        DocumentReference docRef = db.collection("email_verifications").document(emailKey);


        docRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String storedCode = document.getString("code");
                if (storedCode != null && storedCode.equals(code)) {
                    long now = System.currentTimeMillis();
                    Long expiresAt = document.getLong("expiresAt");

                    if (expiresAt != null && now > expiresAt) {
                        showOnScreenError("Code Expired. Please try again.");
                        clearCode();
                        resendLink.setEnabled(true);
                    } else {
                        docRef.update("verified", true)
                                .addOnSuccessListener(aVoid -> {
                                    showDialog(R.drawable.ic_check, "Success!", "You successfully created your account..");
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (resendTimer != null) resendTimer.cancel();
                                        Intent intent = new Intent(CA_Confirmation.this, SignInPage.class);
                                        intent.putExtra("email", email);
                                        startActivity(intent);
                                        finish();
                                    }, 2000);
                                });
                    }
                }
                else {
                    showOnScreenError("Wrong Code. Please try again.");

                    clearCode();

                }
            } else {
                android.util.Log.e("CODE_ERROR", "No verification record found.");

//                showDialog(R.drawable.ic_error, "Error", "No verification record found.");
            }
            buttonConfirm.setEnabled(true);
            buttonConfirm.setText("Confirm");
        }).addOnFailureListener(e -> {
//            showDialog(R.drawable.ic_error, "Error", "Failed to fetch code: " + e.getMessage());
            buttonConfirm.setEnabled(true);
            buttonConfirm.setText("Confirm");
        });
    }

    private void resendCode() {
        resendLink.setEnabled(false);
        textError.setVisibility(View.GONE); // Hide error on resend attempt

        String emailKey = email.replace(".", "_");
        String newCode = String.format("%06d", (int) (Math.random() * 999999));
        long newExpiryTime = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes from now

        Map<String, Object> codeData = new HashMap<>();
        codeData.put("code", newCode);
        codeData.put("expiresAt", newExpiryTime);

        db.collection("email_verifications").document(emailKey)
                .set(codeData)
                .addOnSuccessListener(unused -> {
                    StringRequest request = new StringRequest(Request.Method.POST, "https://sttherese-api.onrender.com/send_verification_code.php",
                            response -> {
                                Toast.makeText(this, "New verification code sent!", Toast.LENGTH_LONG).show();
                                startResendTimer();
                                clearCode();
                            },
                            error -> {
                                Toast.makeText(this, "Failed to send email: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                resendLink.setEnabled(true);
                            }) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> params = new HashMap<>();
                            params.put("email", email);
                            params.put("first_name", "User"); // You might need to retrieve the user's name
                            params.put("code", newCode);
                            return params;
                        }
                    };

                    // Volley is not initialized here, assuming you add it or use another pattern
                    Volley.newRequestQueue(this).add(request);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update code record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resendLink.setEnabled(true);
                });
    }

    private void startResendTimer() {
        // Cancel existing timer if active
        if (resendTimer != null) {
            resendTimer.cancel();
        }

        // 1. Disable the resend link
        resendLink.setEnabled(false);
        resendLink.setTextColor(getResources().getColor(R.color.text_secondary)); // Optional: change color

        // 2. Show the countdown TextView
        textResendTimer.setVisibility(View.VISIBLE);

        // 3. Create and start the timer
        resendTimer = new CountDownTimer(TIMER_DURATION_MS, 1000) { // 60s, ticks every 1s
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                textResendTimer.setText(String.format(Locale.getDefault(), "Resend in %ds", seconds));
            }

            @Override
            public void onFinish() {
                // 4. When finished, hide countdown and re-enable link
                textResendTimer.setVisibility(View.GONE);
                resendLink.setEnabled(true);
                resendLink.setTextColor(getResources().getColor(R.color.red_primary)); // Set back to primary color
                resendLink.setText("Resend Code"); // Reset link text
            }
        }.start();
    }
    private void showOnScreenError(String message) {
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
        // Ensure the timer is still running if the error occurs before it finishes
        if (resendTimer != null) {
            textResendTimer.setVisibility(View.VISIBLE);
        }
    }
    private void clearCode() {
        d1.setText(""); d2.setText(""); d3.setText("");
        d4.setText(""); d5.setText(""); d6.setText("");
        d1.requestFocus();
    }

    private void showDialog(int icon, String title, String message) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_status, null);
        ((ImageView) dialogView.findViewById(R.id.dialog_icon)).setImageResource(icon);
        ((TextView) dialogView.findViewById(R.id.dialog_title)).setText(title);
        ((TextView) dialogView.findViewById(R.id.dialog_message)).setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setView(dialogView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.dialog_close_btn).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Crucial: Cancel the timer to prevent memory leaks when the activity is destroyed
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}
