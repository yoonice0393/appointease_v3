package com.example.sttherese;

import com.example.sttherese.patient.activities.Home;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.sttherese.doctor.DoctorHomeActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;

import java.util.HashMap;
import java.util.Map;

public class SignInPage extends AppCompatActivity {

    EditText editTextEmail, editTextPassword;
    Button buttonSignIn;
    TextView signUpText, forgotPassword;
    ImageView iconFacebook, iconGoogle;

    boolean passwordVisible = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_page);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        signUpText = findViewById(R.id.textSignUp);
        forgotPassword = findViewById(R.id.textForgotPassword);

        editTextPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable[] drawables = editTextPassword.getCompoundDrawables();
                if (drawables[2] != null) {
                    int drawableWidth = drawables[2].getBounds().width();
                    if (event.getRawX() >= (editTextPassword.getRight() - drawableWidth - editTextPassword.getPaddingEnd())) {
                        togglePasswordVisibility();
                        return true;
                    }
                }
            }
            return false;
        });

        buttonSignIn.setOnClickListener(v -> Login());
        signUpText.setOnClickListener(v -> startActivity(new Intent(SignInPage.this, FindRecord.class)));
        forgotPassword.setOnClickListener(v -> startActivity(new Intent(SignInPage.this, FP_FindAccount.class)));
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            editTextPassword.setTransformationMethod(null);
            editTextPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
        } else {
            editTextPassword.setTransformationMethod(new android.text.method.PasswordTransformationMethod());
            editTextPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_slash, 0);
        }
        editTextPassword.setSelection(editTextPassword.getText().length());
    }

    private void Login() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSignIn.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    buttonSignIn.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchUserRoleAndRedirect(user.getUid(), email);
                        }
                    } else {
                        Toast.makeText(SignInPage.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserRoleAndRedirect(String userId, String email) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("user_role_type");
                        if (role == null) role = "patient";
                        role = role.toLowerCase();

                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("user_doc_id", userId);
                        editor.putString("email", email);
                        editor.putString("user_role_type", role);
                        editor.apply();

                        if ("doctor".equals(role)) {
                            lookupDoctorIdAndRedirect(userId);
                        } else {
                            lookupPatientIdAndRedirect(userId);
                        }
                    }
                });
    }

    private void lookupDoctorIdAndRedirect(String authUid) {
        db.collection("doctors").whereEqualTo("user_id", authUid).limit(1).get()
                .addOnSuccessListener(querySnapshots -> {
                    if (!querySnapshots.isEmpty()) {
                        String docId = querySnapshots.getDocuments().get(0).getId();
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                .putString("doctor_doc_id", docId).apply();
                    }
                    startActivity(new Intent(SignInPage.this, DoctorHomeActivity.class));
                    finish();
                });
    }

    private void lookupPatientIdAndRedirect(String authUid) {
        db.collection("patients").whereEqualTo("userId", authUid).limit(1).get()
                .addOnSuccessListener(querySnapshots -> {
                    if (!querySnapshots.isEmpty()) {
                        String patId = querySnapshots.getDocuments().get(0).getId();
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                .putString("patient_firestore_id", patId).apply();
                    }
                    startActivity(new Intent(SignInPage.this, Home.class));
                    finish();
                });
    }
}
