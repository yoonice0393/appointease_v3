package com.example.sttherese;

import android.os.Bundle;
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Terms_Conditions extends AppCompatActivity {
    TextView termsContent;
    ImageView backBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        // Find the TextView and set HTML content
        termsContent = findViewById(R.id.terms_content);
        backBtn = findViewById(R.id.buttonBack);

        termsContent.setText(Html.fromHtml(getString(R.string.terms), Html.FROM_HTML_MODE_LEGACY));

        backBtn.setOnClickListener(v -> onBackPressed());
    }
}