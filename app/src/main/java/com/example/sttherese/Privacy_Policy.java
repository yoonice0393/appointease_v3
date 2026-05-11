package com.example.sttherese;

import android.os.Bundle;
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Privacy_Policy extends AppCompatActivity {

    TextView termsContent;
    ImageView backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_privacy_policy);
        // Find the TextView and set HTML content
        termsContent = findViewById(R.id.terms_content);
        backBtn = findViewById(R.id.buttonBack);

        termsContent.setText(Html.fromHtml(getString(R.string.privacy), Html.FROM_HTML_MODE_LEGACY));

        backBtn.setOnClickListener(v -> onBackPressed());
    }
}