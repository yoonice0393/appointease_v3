package com.example.sttherese;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.sttherese.patient.activities.ResetMethodEmail;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FP_FindAccount extends AppCompatActivity {

    EditText editTextEmail;
    MaterialButton buttonContinue;
    ImageView backBtn;

  
    private static final String API_URL = "https://sttherese-api.onrender.com/send_password_reset_code.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fp_find_account);

        editTextEmail = findViewById(R.id.editTextEmail);
        buttonContinue = findViewById(R.id.buttonContinue);
        backBtn = findViewById(R.id.buttonBack);

        buttonContinue.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();

            if (email.isEmpty()) {
                editTextEmail.setError("Please enter your email");
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editTextEmail.setError("Invalid email address");
                return;
            }

            buttonContinue.setEnabled(false);
            buttonContinue.setText("Sending code...");

            sendResetCode(email);
        });

        backBtn.setOnClickListener(v -> finish());
    }

    private void sendResetCode(String email) {
        // Use StringRequest as the server expects application/x-www-form-urlencoded data in $_POST
        StringRequest request = new StringRequest(
                Request.Method.POST,
                API_URL,
                response -> {

                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.optString("message", "Request completed.");

                        if (success) {
                            Toast.makeText(this, "Code sent! Check your email.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(FP_FindAccount.this, ResetMethodEmail.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    buttonContinue.setEnabled(true);
                    buttonContinue.setText("Send Code");
                },
                error -> {
                    String msg = "Network error";
                    if (error.networkResponse != null) {
                        msg = "Error: " + error.networkResponse.statusCode;
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            android.util.Log.e("FP_FindAccount", "Error response: " + responseBody);

                            // Try to parse the error JSON from the 400 response
                            JSONObject errorJson = new JSONObject(responseBody);
                            if (errorJson.has("error")) {
                                msg = errorJson.getString("error");
                            } else {
                                msg = "Error: " + error.networkResponse.statusCode;
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Response format error. Check server logs.", Toast.LENGTH_LONG).show();
                        }
                    }
                    Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show();
                    buttonContinue.setEnabled(true);
                    buttonContinue.setText("Send Code");
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                // This method creates the application/x-www-form-urlencoded body for $_POST
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                // Also add the 'action' parameter expected by the PHP router
                params.put("action", "request_reset");
                return params;
            }

        };

        // 🎯 FIX: Increase timeout to handle server cold start
        int socketTimeout = 30000; // 30 seconds
        RetryPolicy policy = new DefaultRetryPolicy(
                socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        );
        request.setRetryPolicy(policy);

        Volley.newRequestQueue(this).add(request);
    }
}
