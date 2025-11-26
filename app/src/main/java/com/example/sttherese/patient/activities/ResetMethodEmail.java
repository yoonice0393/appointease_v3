package com.example.sttherese.patient.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.sttherese.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ResetMethodEmail extends AppCompatActivity {
    private static final String TAG = "ResetMethodEmail";
    private static final String API_URL = "https://sttherese-api.onrender.com/send_password_reset_code.php";

    EditText[] codeFields = new EditText[6];
    Button buttonConfirm;
    TextView textResendCode, textTimer, textError; // textTimer is mapped to textResendTimer in XML

    DatabaseReference database;
    String email;
    CountDownTimer countDownTimer;
    final long TIMER_DURATION_MS = 60000; // 60 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_method_email);

        // Initialize views
        codeFields[0] = findViewById(R.id.codeDigit1);
        codeFields[1] = findViewById(R.id.codeDigit2);
        codeFields[2] = findViewById(R.id.codeDigit3);
        codeFields[3] = findViewById(R.id.codeDigit4);
        codeFields[4] = findViewById(R.id.codeDigit5);
        codeFields[5] = findViewById(R.id.codeDigit6);
        buttonConfirm = findViewById(R.id.buttonConfirm);
        textResendCode = findViewById(R.id.textResendCode);
        textTimer = findViewById(R.id.textResendTimer);
        textError = findViewById(R.id.textError);
        textError.setVisibility(View.GONE);


        // Initialize Firebase Realtime Database
        database = FirebaseDatabase.getInstance("https://appointease-7aa63-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("password_resets");
        email = getIntent().getStringExtra("email");

        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Error: No email provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupAutoFocus();
        startResendTimer();

        buttonConfirm.setOnClickListener(v -> verifyCode());

        textResendCode.setOnClickListener(v -> resendCode());
    }

    private void verifyCode() {
        String enteredCode = getFullCode();

        if (enteredCode.length() != 6) {
            Toast.makeText(this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonConfirm.setEnabled(false);
        textError.setVisibility(View.GONE);

        // Sanitize email for database key
        String docId = email.replace(".", "_").replace("@", "_at_");

        Log.d(TAG, "Verifying code for email: " + email);

        database.child(docId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String storedCode = dataSnapshot.child("code").getValue(String.class);
                    Long expiresAt = dataSnapshot.child("expires_at").getValue(Long.class);
                    Long attempts = dataSnapshot.child("attempts").getValue(Long.class);

                    // Check expiry
                    if (expiresAt != null && System.currentTimeMillis() / 1000 > expiresAt) {
                        buttonConfirm.setEnabled(true);
                        showOnScreenError("Code Expired. Please request a new one.");
                        clearCodeFields();
                        return;
                    }

                    // Check attempts
                    if (attempts != null && attempts >= 5) {
                        buttonConfirm.setEnabled(true);
                        showOnScreenError("Maximum attempts exceeded. Please resend the code.");
                        return;
                    }

                    // Verify code
                    if (storedCode != null && storedCode.equals(enteredCode)) {
                        // Mark as verified
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("verified", true);
                        updates.put("verified_at", System.currentTimeMillis() / 1000);

                        database.child(docId).updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    showCustomDialog(R.drawable.ic_check, "Success!", "Code has been verified.");

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (countDownTimer != null) countDownTimer.cancel();
                                        Intent intent = new Intent(ResetMethodEmail.this, ChangePassword.class);
                                        intent.putExtra("email", email);
                                        intent.putExtra("verified", true);
                                        intent.putExtra("code", storedCode);
                                        startActivity(intent);
                                        finish();
                                    }, 2000);
                                });
                    } else {
                        // Incorrect code
                        long newAttempts = (attempts != null ? attempts : 0) + 1;
                        database.child(docId).child("attempts").setValue(newAttempts);

                        buttonConfirm.setEnabled(true);
                        clearCodeFields();
                        codeFields[0].requestFocus();
                        showOnScreenError("Wrong Code. Attempts: " + newAttempts + "/5");
                    }
                } else {
                    buttonConfirm.setEnabled(true);
                    Log.e(TAG, "No data found in database for: " + docId);
                    showCustomDialog(R.drawable.ic_error, "Error", "No verification found. Please restart.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                buttonConfirm.setEnabled(true);
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                Toast.makeText(ResetMethodEmail.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendCode() {
        if (!textResendCode.isEnabled()) {
            Toast.makeText(this, "Please wait before resending the code.", Toast.LENGTH_SHORT).show();
            return;
        }

        textResendCode.setEnabled(false);
        textError.setVisibility(View.GONE);
        Toast.makeText(this, "Sending new code...", Toast.LENGTH_SHORT).show();

        // Use StringRequest
        StringRequest request = new StringRequest(
                Request.Method.POST,
                API_URL,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");

                        if (success) {
                            Toast.makeText(this, "New code sent!", Toast.LENGTH_SHORT).show();
                            startResendTimer();
                            clearCodeFields();
                        } else {
                            String message = jsonResponse.optString("message", "Failed to send code");
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            textResendCode.setEnabled(true); // Re-enable if API fails immediately
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        textResendCode.setEnabled(true); // Re-enable on parse error
                    }
                },
                error -> {
                    String msg = "Failed to send code. Network error.";
                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Resend error: " + responseBody);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    textResendCode.setEnabled(true); // Re-enable on network error
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void startResendTimer() {
        // 1. Cancel existing timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // 2. Disable the resend link/change appearance
        textResendCode.setEnabled(false);
        textResendCode.setAlpha(0.5f);
        textResendCode.setText("Resend Code");
        textResendCode.setVisibility(View.GONE);

        // 3. Show the countdown TextView
        textTimer.setVisibility(View.VISIBLE);

        // 4. Create and start the timer
        countDownTimer = new CountDownTimer(TIMER_DURATION_MS, 1000) { // 60s, ticks every 1s
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                textTimer.setText(String.format(Locale.getDefault(), "Resend in %ds", seconds));
            }

            @Override
            public void onFinish() {
                // 5. When finished, hide countdown and re-enable link
                textTimer.setVisibility(View.GONE);
                textResendCode.setEnabled(true);
                textResendCode.setAlpha(1.0f);
                textResendCode.setText("Resend Code");
            }
        }.start();
    }

    private void showOnScreenError(String message) {
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);

        // Check if the timer is currently running
        // If countDownTimer is NOT null, and the timer TextView is visible, the timer is active.
        // If textTimer is GONE, the timer has finished.
        if (textTimer.getVisibility() == View.VISIBLE) {
            // The timer is active (still counting down), keep the timer text visible.
            textTimer.setVisibility(View.VISIBLE);
            textResendCode.setVisibility(View.GONE);
        } else {
            // The timer has finished (onFinish was called), show the resend link.
            textResendCode.setVisibility(View.VISIBLE);
            textTimer.setVisibility(View.GONE);
        }
    }

    private String getFullCode() {
        StringBuilder builder = new StringBuilder();
        for (EditText field : codeFields) {
            builder.append(field.getText().toString().trim());
        }
        return builder.toString();
    }

    private void clearCodeFields() {
        for (EditText field : codeFields) {
            field.setText("");
        }
        if (codeFields.length > 0) {
            codeFields[0].requestFocus();
        }
    }

    private void setupAutoFocus() {
        for (int i = 0; i < codeFields.length; i++) {
            EditText current = codeFields[i];
            final int currentIndex = i;

            current.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Move focus forward when a digit is entered
                    if (s.length() == 1 && currentIndex < codeFields.length - 1) {
                        codeFields[currentIndex + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Add key listener to handle backspace/delete correctly
            current.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (codeFields[currentIndex].getText().toString().isEmpty() && currentIndex > 0) {
                        codeFields[currentIndex - 1].requestFocus();
                        codeFields[currentIndex - 1].setText("");
                    }
                }
                return false;
            });
        }
    }

    private void showCustomDialog(int iconResId, String title, String message) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_status, null);
        ImageView icon = dialogView.findViewById(R.id.dialog_icon);
        TextView titleText = dialogView.findViewById(R.id.dialog_title);
        TextView msg = dialogView.findViewById(R.id.dialog_message);
        Button closeBtn = dialogView.findViewById(R.id.dialog_close_btn);

        icon.setImageResource(iconResId);
        titleText.setText(title);
        msg.setText(message);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Crucial: Cancel the timer to prevent memory leaks
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}