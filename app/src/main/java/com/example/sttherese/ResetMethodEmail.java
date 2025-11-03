package com.example.sttherese;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ResetMethodEmail extends AppCompatActivity {
    private static final String TAG = "ResetMethodEmail";
    private static final String API_URL = "https://sttherese-api.onrender.com/send_password_reset_code.php";

    EditText[] codeFields = new EditText[6];
    Button buttonConfirm;
    TextView textResendCode, textTimer;

    DatabaseReference database; // Changed from Firestore to Realtime Database
    String email;
    CountDownTimer countDownTimer;
    boolean canResend = false;

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
        textTimer = findViewById(R.id.textTimer);

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

        textResendCode.setOnClickListener(v -> {
            if (canResend) {
                resendCode();
            } else {
                Toast.makeText(this, "Please wait before resending", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyCode() {
        String enteredCode = getFullCode();

        if (enteredCode.length() != 6) {
            Toast.makeText(this, "Please enter the full 6-digit code", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonConfirm.setEnabled(false);

        // Sanitize email for database key
        String docId = email.replace(".", "_").replace("@", "_at_");

        Log.d(TAG, "Verifying code for email: " + email);
        Log.d(TAG, "Database path: password_resets/" + docId);
        Log.d(TAG, "Entered code: " + enteredCode);

        database.child(docId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Database snapshot exists: " + dataSnapshot.exists());

                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Snapshot data: " + dataSnapshot.getValue());

                    String storedCode = dataSnapshot.child("code").getValue(String.class);
                    Long expiresAt = dataSnapshot.child("expires_at").getValue(Long.class);
                    Boolean verified = dataSnapshot.child("verified").getValue(Boolean.class);
                    Long attempts = dataSnapshot.child("attempts").getValue(Long.class);

                    Log.d(TAG, "Stored code: " + storedCode);
                    Log.d(TAG, "Expires at: " + expiresAt);
                    Log.d(TAG, "Verified: " + verified);
                    Log.d(TAG, "Attempts: " + attempts);

                    // Check if already used
                    if (verified != null && verified) {
                        buttonConfirm.setEnabled(true);
                        showCustomDialog(R.drawable.ic_error, "Code Used", "This code has already been used.");
                        return;
                    }

                    // Check expiry
                    if (expiresAt != null && System.currentTimeMillis() / 1000 > expiresAt) {
                        buttonConfirm.setEnabled(true);
                        showCustomDialog(R.drawable.ic_error, "Code Expired", "Code has expired. Please request a new one.");
                        clearCodeFields();
                        return;
                    }

                    // Check attempts
                    if (attempts != null && attempts >= 5) {
                        buttonConfirm.setEnabled(true);
                        showCustomDialog(R.drawable.ic_error, "Too Many Attempts", "Maximum attempts exceeded.");
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
                                    showCustomDialog(R.drawable.ic_check, "Success!", "Code verified!");

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
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
                        showCustomDialog(R.drawable.ic_error, "Incorrect Code", "Attempts: " + newAttempts + "/5");
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
                Log.e(TAG, "Error code: " + databaseError.getCode());
                Log.e(TAG, "Error details: " + databaseError.getDetails());
                Toast.makeText(ResetMethodEmail.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendCode() {
        Toast.makeText(this, "Sending new code...", Toast.LENGTH_SHORT).show();

        // Use StringRequest to match FP_FindAccount format
        com.android.volley.toolbox.StringRequest request = new com.android.volley.toolbox.StringRequest(
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
                            codeFields[0].requestFocus();
                        } else {
                            String message = jsonResponse.optString("message", "Failed to send code");
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String msg = "Network error";
                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Resend error: " + responseBody);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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

    // Keep all other methods the same (getFullCode, clearCodeFields, setupAutoFocus, showCustomDialog, startResendTimer, onDestroy)

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
    }

    private void setupAutoFocus() {
        for (int i = 0; i < codeFields.length; i++) {
            EditText current = codeFields[i];
            int currentIndex = i;

            current.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && currentIndex < codeFields.length - 1) {
                        codeFields[currentIndex + 1].requestFocus();
                    } else if (s.length() == 0 && currentIndex > 0) {
                        codeFields[currentIndex - 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
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

    private void startResendTimer() {
        canResend = false;
        textResendCode.setEnabled(false);
        textResendCode.setAlpha(0.5f);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (textTimer != null) {
                    textTimer.setText("Resend in " + (millisUntilFinished / 1000) + "s");
                }
            }

            @Override
            public void onFinish() {
                canResend = true;
                if (textTimer != null) {
                    textTimer.setText("Code not received?");
                }
                textResendCode.setEnabled(true);
                textResendCode.setAlpha(1.0f);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}