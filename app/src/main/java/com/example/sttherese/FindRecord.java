package com.example.sttherese;

import android.app.DatePickerDialog;
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

import java.util.Calendar;
import java.util.Locale;

public class FindRecord extends AppCompatActivity {

    private static final String TAG = "FindRecordActivity";

    EditText etFirstName, etLastName, etDob;
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
        etFirstName = findViewById(R.id.editTextFirstName);
        etLastName = findViewById(R.id.editTextLastName);
        etDob = findViewById(R.id.editTextDob);
        textSignIn=findViewById(R.id.textSignIn);
        backBtn = findViewById(R.id.buttonBack);

        db = FirebaseFirestore.getInstance();
        patientsRef = db.collection("existing_patients");

        // Continue button click
        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstName = etFirstName.getText().toString().trim();
                String lastName = etLastName.getText().toString().trim();
                String dob = etDob.getText().toString().trim();

                // 1. Initial check for emptiness
                if (firstName.isEmpty()) {
                    etFirstName.setError("First name is required");
                    etFirstName.requestFocus();
                    return;
                }

                if (lastName.isEmpty()) {
                    etLastName.setError("Last name is required");
                    etLastName.requestFocus();
                    return;
                }

                if (dob.isEmpty()) {
                    etDob.setError("Birthday is required");
                    etDob.requestFocus();
                    return;
                }

                findRecord(firstName, lastName, dob);
            }
        });

        etDob.setOnClickListener(v -> showDatePicker());

        textSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(FindRecord.this, SignInPage.class);
            startActivity(intent);
            finish();
        });

        // Back button
        backBtn.setOnClickListener(v -> onBackPressed());
    }

    private void findRecord(String firstName, String lastName, String dob) {
        Toast.makeText(this, "Searching record...", Toast.LENGTH_SHORT).show();

        // Firestore does exact filtering by birthday, then local comparison handles case-insensitive names.
        patientsRef.whereEqualTo("dob", dob)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int matchCount = 0;
                        QueryDocumentSnapshot matchedDocument = null;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String dbFirst = document.getString("first_name");
                            String dbLast = document.getString("last_name");
                            String dbDob = document.getString("dob");

                            String dbFirstNormalized = normalizeName(dbFirst);
                            String dbLastNormalized = normalizeName(dbLast);
                            String enteredFirst = normalizeName(firstName);
                            String enteredLast = normalizeName(lastName);

                            if (enteredFirst.equals(dbFirstNormalized)
                                    && enteredLast.equals(dbLastNormalized)
                                    && dob.equals(dbDob)) {
                                matchCount++;
                                matchedDocument = document;
                            }
                        }

                        if (matchCount == 0) {
                            Toast.makeText(FindRecord.this, "No matching record found. Please contact the clinic.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (matchCount > 1) {
                            Toast.makeText(FindRecord.this, "Multiple matching records found. Please contact the clinic for verification.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        Boolean accountCreated = matchedDocument.getBoolean("account_created");
                        String linkedUserId = matchedDocument.getString("linked_user_id");
                        if (Boolean.TRUE.equals(accountCreated) || (linkedUserId != null && !linkedUserId.trim().isEmpty())) {
                            Toast.makeText(FindRecord.this, "This clinic record already has an account. Please sign in or contact the clinic.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        Intent intent = new Intent(FindRecord.this, CA_PersonalDetails.class);
                        intent.putExtra("existing_patient_id", matchedDocument.getId());
                        intent.putExtra("first_name", matchedDocument.getString("first_name"));
                        intent.putExtra("last_name", matchedDocument.getString("last_name"));
                        intent.putExtra("dob", matchedDocument.getString("dob"));

                        startActivity(intent);
                        finish();
                    } else {
                        // Handle the case where the query failed
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        Log.e(TAG, "Firestore Query Failed: ", task.getException());
//                        Toast.makeText(FindRecord.this, "Error: Failed to connect to database.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    etDob.setText(formattedDate);
                },
                year, month, day
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.getDefault());
    }
}
