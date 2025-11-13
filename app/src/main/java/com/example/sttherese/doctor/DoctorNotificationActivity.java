package com.example.sttherese.doctor;

import android.os.Bundle;
import android.widget.ImageView;


import androidx.appcompat.app.AppCompatActivity;

import com.example.sttherese.R;

public class DoctorNotificationActivity extends AppCompatActivity {

    ImageView backBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_doctor_notification);

        backBtn = findViewById(R.id.buttonBack);

        backBtn.setOnClickListener(v -> onBackPressed());
    }
}