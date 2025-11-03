package com.example.sttherese;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class CA_PersonalDetails extends AppCompatActivity {

    EditText editTextFirstName, editTextMiddleName, editTextLastName;
    EditText editTextDOB, editTextContactNumber, editTextAddress;
    Button buttonSignUp;
    ImageView backBtn;

    RadioGroup genderRadioGroup;
    RadioButton maleRadioButton, femaleRadioButton, otherRadioButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ca_personal_details);

        // Initialize views
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextMiddleName = findViewById(R.id.editTextMiddleName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextDOB = findViewById(R.id.editTextDOB);
        genderRadioGroup = findViewById(R.id.genderRadioGroup);
        maleRadioButton = findViewById(R.id.maleRadioButton);
        femaleRadioButton = findViewById(R.id.femaleRadioButton);
        otherRadioButton = findViewById(R.id.otherrRadioButton);
        editTextContactNumber = findViewById(R.id.editTextContactNumber);
        editTextAddress = findViewById(R.id.editTextAddress);
        buttonSignUp = findViewById(R.id.buttonContinue);
        backBtn = findViewById(R.id.buttonBack);



        Intent intent = getIntent();
        if (intent != null) {
            editTextFirstName.setText(intent.getStringExtra("first_name"));
            editTextMiddleName.setText(intent.getStringExtra("middle_name"));
            editTextLastName.setText(intent.getStringExtra("last_name"));
            editTextDOB.setText(intent.getStringExtra("dob"));
            editTextContactNumber.setText(intent.getStringExtra("contact"));
            editTextAddress.setText(intent.getStringExtra("address"));
        }

        // Make DOB field non-editable and show date picker on click
        editTextDOB.setFocusable(false);
        editTextDOB.setClickable(true);
        editTextDOB.setOnClickListener(v -> showDatePicker());

        // Button to proceed to Account Credentials
        buttonSignUp.setOnClickListener(v -> validateAndProceed());
        backBtn.setOnClickListener(v -> onBackPressed());

    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Format: YYYY-MM-DD for database compatibility
                    String formattedDate = String.format("%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    editTextDOB.setText(formattedDate);
                },
                year, month, day
        );

        // Set maximum date to today (user must be born before today)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // Optional: Set minimum date (e.g., 100 years ago)
        calendar.add(Calendar.YEAR, -100);
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

        datePickerDialog.show();
    }

    private void validateAndProceed() {
        String firstName = editTextFirstName.getText().toString().trim();
        String middleName = editTextMiddleName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String dob = editTextDOB.getText().toString().trim();
        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
        String contactNumber = editTextContactNumber.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();


        if (selectedGenderId == -1) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedGenderButton = findViewById(selectedGenderId);
        String gender = selectedGenderButton.getText().toString();
        
        // Validation with specific error messages
        if (firstName.isEmpty()) {
            editTextFirstName.setError("First name is required");
            editTextFirstName.requestFocus();
            return;
        }

        if (lastName.isEmpty()) {
            editTextLastName.setError("Last name is required");
            editTextLastName.requestFocus();
            return;
        }

        if (dob.isEmpty()) {
            editTextDOB.setError("Date of birth is required");
            editTextDOB.requestFocus();
            Toast.makeText(this, "Please select your date of birth", Toast.LENGTH_SHORT).show();
            return;
        }

        if (contactNumber.isEmpty()) {
            editTextContactNumber.setError("Contact number is required");
            editTextContactNumber.requestFocus();
            return;
        }


        // Validate Philippine mobile number format
        // Should be 11 digits starting with 09
        if (!isValidPhilippineNumber(contactNumber)) {
            editTextContactNumber.setError("Please enter a valid Philippine mobile number (e.g., 09123456789)");
            editTextContactNumber.requestFocus();
            return;
        }

        if (address.isEmpty()) {
            editTextAddress.setError("Address is required");
            editTextAddress.requestFocus();
            return;
        }

        // All validations passed, proceed to Account Credentials
        Intent intent = new Intent(CA_PersonalDetails.this, CA_AccountCredentials.class);
        intent.putExtra("first_name", firstName);
        intent.putExtra("middle_name", middleName);
        intent.putExtra("last_name", lastName);
        intent.putExtra("dob", dob);
        intent.putExtra("gender", gender);
        intent.putExtra("contact", contactNumber);
        intent.putExtra("address", address);
        startActivity(intent);
    }

    private boolean isValidPhilippineNumber(String number) {
        // Remove any spaces, dashes, or parentheses
        String cleaned = number.replaceAll("[\\s\\-()]", "");

        // Check if it's 11 digits starting with 09
        if (cleaned.matches("^09\\d{9}$")) {
            return true;
        }

        // Alternative: Check if it's 10 digits starting with 9 (accept without leading 0)
        if (cleaned.matches("^9\\d{9}$")) {
            return true;
        }

        // You can also accept landline format or international format
        // For now, we'll keep it strict to mobile numbers

        return false;
    }
}