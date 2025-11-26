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

    private int calculateAge(String dobString) {
        if (dobString == null || dobString.isEmpty()) {
            return 0;
        }

        // Assuming dobString is in "YYYY-MM-DD" format as set in showDatePicker()
        try {
            String[] parts = dobString.split("-");
            int birthYear = Integer.parseInt(parts[0]);
            int birthMonth = Integer.parseInt(parts[1]);
            int birthDay = Integer.parseInt(parts[2]);

            Calendar dob = Calendar.getInstance();
            dob.set(birthYear, birthMonth - 1, birthDay); // Note: Month is 0-indexed

            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            // Adjust age if birthday hasn't occurred this year yet
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;

        } catch (Exception e) {
            // Handle potential parsing error (shouldn't happen if format is consistent)
            return 0;
        }
    }

    private void validateAndProceed() {
        String firstName = editTextFirstName.getText().toString().trim();
        String middleName = editTextMiddleName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String dob = editTextDOB.getText().toString().trim();
        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
        String contactNumber = editTextContactNumber.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();

        // --- 1. Required Fields (Text Inputs) ---
        // Prioritize fields that are always mandatory.

        if (firstName.isEmpty()) {
            editTextFirstName.setError("First name is required");
            editTextFirstName.requestFocus();
            return;
        }

        // Middle name is often optional, so no check is needed here unless it's required.

        if (lastName.isEmpty()) {
            editTextLastName.setError("Last name is required");
            editTextLastName.requestFocus();
            return;
        }

        if (dob.isEmpty()) {
            // Note: Use setError for the field, and Toast for overall instruction.
            editTextDOB.setError("Date of birth is required");
            editTextDOB.requestFocus();
            Toast.makeText(this, "Please select your date of birth.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- 2. Required Fields (Radio Group) ---
        if (selectedGenderId == -1) {
            // Note: Since a RadioGroup doesn't have an setError method, a Toast is the best approach.
            Toast.makeText(this, "Please select your gender.", Toast.LENGTH_SHORT).show();
            // You can also add a visual cue to the RadioGroup/label if available.
            return;
        }
        // Safely get gender *after* the check passes
        RadioButton selectedGenderButton = findViewById(selectedGenderId);
        String gender = selectedGenderButton.getText().toString();


        // --- 3. Logic & Format Checks (Age) ---

        int age = calculateAge(dob);
        final int MINIMUM_REGISTRATION_AGE = 18;

        // Check A: Ensure age is valid (0 often means calculation error or future date)
        if (age <= 0) {
            Toast.makeText(this, "Invalid date of birth selected.", Toast.LENGTH_LONG).show();
//            editTextDOB.setError("Invalid DOB or future date selected.");
            editTextDOB.requestFocus();
            return;
        }

        // Check B: Minimum age restriction
        if (age < MINIMUM_REGISTRATION_AGE) {
            String message = "You must be at least " + MINIMUM_REGISTRATION_AGE + " years old to create an account.";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
//            editTextDOB.setError("Must be " + MINIMUM_REGISTRATION_AGE + " years old.");
            editTextDOB.requestFocus();
            return;
        }


        // --- 4. Contact & Address Checks ---

        if (contactNumber.isEmpty()) {
            editTextContactNumber.setError("Contact number is required");
            editTextContactNumber.requestFocus();
            return;
        }

        // Validate Philippine mobile number format *after* checking if it's empty
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

        // --- 5. All validations passed, proceed ---
        Intent intent = new Intent(CA_PersonalDetails.this, CA_AccountCredentials.class);
        intent.putExtra("first_name", firstName);
        intent.putExtra("middle_name", middleName);
        intent.putExtra("last_name", lastName);
        intent.putExtra("dob", dob);
        intent.putExtra("gender", gender);
        intent.putExtra("age", age);
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