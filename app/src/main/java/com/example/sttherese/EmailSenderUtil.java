package com.example.sttherese;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to handle the asynchronous Volley request for sending the password reset code email.
 * This centralizes the logic, preventing duplication across activities.
 */
public class EmailSenderUtil {

    private static final String TAG = "EmailSenderUtil";
    // CRITICAL UPDATE: REPLACE 'YOUR-DOMAIN-HERE.com' with your actual InfinityFree public domain!
    // Example: "https://st-therese-app.infinityfreeapp.com/user_api/send_password_reset_code.php"
    private static final String API_URL = "https://st-therese-api.ct.ws/user_api/send_password_reset_code.php";

    /**
     * Sends the 6-digit verification code to the user's email via the custom PHP backend.
     *
     * @param context The application context (usually the calling Activity) for Volley and Toast.
     * @param email The recipient's email address.
     * @param code The 6-digit verification code.
     */
    public static void sendEmailWithCode(Context context, String email, String code) {
        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    Log.d(TAG, "API Response: " + response);
                    Toast.makeText(context, "Verification code sent to your email!", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Log.e(TAG, "Volley Error: " + (error.getMessage() != null ? error.getMessage() : "Unknown Error"));
                    // Added a log for the raw response for better debugging
                    Log.e(TAG, "Volley Network Response: " + (error.networkResponse != null ? new String(error.networkResponse.data) : "No Network Response"));

                    Toast.makeText(context, "Error sending email. Check logs for API details.", Toast.LENGTH_LONG).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                params.put("first_name", "User"); // Assuming a default name for the email template
                params.put("code", code);
                return params;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }
}
