package com.example.sttherese;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

public class FindRecord extends AppCompatActivity {

    private static final String TAG = "FindRecordActivity";

    EditText etName, etContactNumber;
    TextView textSignIn;
    MaterialButton buttonContinue;
    ImageView backBtn;

    // Use FirebaseFirestore for Firestore
    private FirebaseFirestore db;
    private CollectionReference patientsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_record);

        buttonContinue = findViewById(R.id.buttonContinue);
        etName = findViewById(R.id.editTextName);
        textSignIn=findViewById(R.id.textSignIn);
        etContactNumber = findViewById(R.id.editTextContact);
        backBtn = findViewById(R.id.buttonBack);

        db = FirebaseFirestore.getInstance();
        patientsRef = db.collection("existing_patients");

        // Continue button click
        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = etName.getText().toString().trim();
                String contact = etContactNumber.getText().toString().trim();

                // 1. Initial check for emptiness
                if (fullName.isEmpty()) {
                    etName.setError("Full name is required");
                    etName.requestFocus();
                    return;
                }

                if (contact.isEmpty()) {
                    etContactNumber.setError("Contact number is required");
                    etContactNumber.requestFocus();
                    return;
                }

                // 2. Clean the contact number (removes any spaces, dashes, etc.)
                String cleanedContact = contact.replaceAll("[^0-9]", "");

                // 3. Validation Check
                if (!isValidPhilippineNumber(cleanedContact)) {
                    etContactNumber.setError("Please enter a valid 11-digit number (e.g., 09123456789)");
                    etContactNumber.requestFocus();
                    return;
                }

                // 4. Validation Passed: Use the CLEANED number for the database query
                findRecord(fullName, cleanedContact);
            }
        });

        textSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(FindRecord.this, SignInPage.class);
            startActivity(intent);
            finish();
        });

        // Back button
        backBtn.setOnClickListener(v -> onBackPressed());
    }

    private void findRecord(String fullName, String contact) {
        Toast.makeText(this, "Searching record...", Toast.LENGTH_SHORT).show();

        // Firestore Query: Filter by 'contact' first for efficiency
        patientsRef.whereEqualTo("contact", contact)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean found = false;

                        // 1. Iterate over the results filtered by contact
                        for (QueryDocumentSnapshot document : task.getResult()) {

                            // 2. Retrieve data using the document.getString() method
                            String dbFirst = document.getString("first_name");
                            String dbMiddle = document.getString("middle_name");
                            String dbLast = document.getString("last_name");
                            String dbContact = document.getString("contact");
                            String dbAddress = document.getString("address");
                            String dbDob = document.getString("dob"); // Assuming DOB is stored as a String

                            // 3. Perform local filtering for the name (since Firestore can't do combined name matching)

                            // Construct the full name from the database fields
                            String combinedName = String.format(Locale.getDefault(), "%s %s", dbFirst, dbLast).trim().toLowerCase();
                            String enteredName = fullName.trim().toLowerCase();

                            if (enteredName.equals(combinedName)) {
                                found = true;

                                // Pass data to next activity
                                Intent intent = new Intent(FindRecord.this, CA_PersonalDetails.class);
                                intent.putExtra("first_name", dbFirst);
                                intent.putExtra("middle_name", dbMiddle);
                                intent.putExtra("last_name", dbLast);
                                intent.putExtra("dob", dbDob);
                                intent.putExtra("contact", dbContact);
                                intent.putExtra("address", dbAddress);

                                startActivity(intent);
                                finish();
                                break;
                            }
                        }

                        if (!found) {
                            Toast.makeText(FindRecord.this, "No matching record found. Please contact the clinic.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Handle the case where the query failed
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        Log.e(TAG, "Firestore Query Failed: ", task.getException());
//                        Toast.makeText(FindRecord.this, "Error: Failed to connect to database.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Checks if the cleaned number is a valid 11-digit Philippine mobile number (starting with 09).
     */
    private boolean isValidPhilippineNumber(String number) {
        // 1. Remove any non-digit characters (in case of spaces/dashes)
        String cleaned = number.replaceAll("[^0-9]", "");

        // 2. Must be exactly 11 digits and start with "09" (Standard PH mobile format)
        if (cleaned.matches("^09\\d{9}$")) {
            return true;
        }

        // Optional: If you allow 10-digit numbers starting with 9 (without the leading 0)
        // if (cleaned.matches("^9\\d{9}$")) {
        //     return true;
        // }

        return false;
    }
}