package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class TemplateSelectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_selection);

        // Back Button logic
        ImageButton backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        // Setup Template Click Listeners
        findViewById(R.id.template1).setOnClickListener(v -> saveResume());
        findViewById(R.id.template2).setOnClickListener(v -> saveResume());
        findViewById(R.id.template3).setOnClickListener(v -> saveResume());
        findViewById(R.id.template4).setOnClickListener(v -> saveResume());
        findViewById(R.id.template5).setOnClickListener(v -> saveResume());
        findViewById(R.id.template6).setOnClickListener(v -> saveResume());
    }

    private void saveResume() {
        // Generate current metadata
        String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
        String userEmail = "user@example.com";

        // Create the Resume object with "My Resume" as the title
        Resume newResume = new Resume("My Resume", userEmail, date);

        Executors.newSingleThreadExecutor().execute(() -> {
            // SINGLE INSERTION POINT: This happens only once when a template is clicked
            AppDatabase.getInstance(TemplateSelectionActivity.this).resumeDao().insert(newResume);

            runOnUiThread(() -> {
                Toast.makeText(this, "Resume Saved Successfully!", Toast.LENGTH_SHORT).show();

                // Navigate to My Resumes list
                Intent intent = new Intent(TemplateSelectionActivity.this, MyResumesActivity.class);

                // FLAG_ACTIVITY_CLEAR_TOP ensures that if the user presses back from the list,
                // they don't loop back into the template picker.
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);
                finish();
            });
        });
    }
}