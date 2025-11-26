// PasswordEntryBottomSheet.java

package com.example.sttherese.doctor;

import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.sttherese.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PasswordEntryBottomSheet extends BottomSheetDialogFragment {

    // 1. Interface for communication back to ProfileActivity
    public interface PasswordVerificationListener {
        void onVerificationSuccess();
    }

    private PasswordVerificationListener listener;

    public void setPasswordVerificationListener(PasswordVerificationListener listener) {
        this.listener = listener;
    }

    // 2. Set the custom theme for rounded corners
    @Override
    public int getTheme() {
        // You MUST have this style defined in your themes.xml (Step 3)
        return R.style.CustomBottomSheetDialogTheme;
    }

    // 3. Inflate the layout (assuming your XML is saved as bottom_sheet_password_entry.xml)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // NOTE: Make sure your XML is saved to R.layout.bottom_sheet_password_entry
        return inflater.inflate(R.layout.dialog_password_verification, container, false);
    }

    // 4. Set up listeners and verification logic
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText passwordInput = view.findViewById(R.id.editTextPassword);
        ImageView togglePassword = view.findViewById(R.id.imageViewTogglePassword);
        MaterialButton proceedBtn = view.findViewById(R.id.buttonProceed);

        // Toggle Password Visibility
        togglePassword.setOnClickListener(v -> {
            if (passwordInput.getTransformationMethod() instanceof PasswordTransformationMethod) {
                passwordInput.setTransformationMethod(null);
                togglePassword.setImageResource(R.drawable.ic_eye);
            } else {
                passwordInput.setTransformationMethod(new PasswordTransformationMethod());
                togglePassword.setImageResource(R.drawable.ic_eye_slash);
            }
            passwordInput.setSelection(passwordInput.getText().length());
        });

        // Proceed Button
        proceedBtn.setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter your password", Toast.LENGTH_SHORT).show();
            } else {
                verifyPassword(password);
            }
        });
    }

    // 5. Verification Logic (Moved from ProfileActivity)
    private void verifyPassword(String password) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            Toast.makeText(getContext(), "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            dismiss();
            return;
        }

        String email = user.getEmail();
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (listener != null) {
                            listener.onVerificationSuccess(); // Notify the activity
                        }
                        Toast.makeText(getContext(), "Access granted", Toast.LENGTH_SHORT).show();
                        dismiss(); // Close the bottom sheet
                    } else {
                        Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                        android.util.Log.e("Auth", "Re-authentication failed: " + task.getException().getMessage());
                    }
                });
    }
}