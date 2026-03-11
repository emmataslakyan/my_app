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

    private int currentResumeId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_selection);

        // Receive the ID from the previous activity
        currentResumeId = getIntent().getIntExtra("RESUME_ID", -1);

        ImageButton backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        // Set listeners for each template button
        findViewById(R.id.template1).setOnClickListener(v -> updateResume("Template 1"));
        findViewById(R.id.template2).setOnClickListener(v -> updateResume("Template 2"));
        findViewById(R.id.template3).setOnClickListener(v -> updateResume("Template 3"));
        findViewById(R.id.template4).setOnClickListener(v -> updateResume("Template 4"));
        findViewById(R.id.template5).setOnClickListener(v -> updateResume("Template 5"));
        findViewById(R.id.template6).setOnClickListener(v -> updateResume("Template 6"));
    }

    private void updateResume(String templateName) {
        String currentDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(TemplateSelectionActivity.this);

            // 1. Fetch the existing resume by its primary key (ID)
            Resume existingResume = db.resumeDao().getResumeById(currentResumeId);

            if (existingResume != null) {
                // 2. Update the existing object
                existingResume.setDate(currentDate);

                // 3. Persist the change via UPDATE (not insert)
                db.resumeDao().update(existingResume);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Template Selected!", Toast.LENGTH_SHORT).show();

                    // Change the destination from MyResumesActivity to ResumePreviewActivity
                    Intent intent = new Intent(TemplateSelectionActivity.this, ResumePreviewActivity.class);

                    // Crucial: Pass the currentResumeId so the preview page knows which data to load
                    intent.putExtra("RESUME_ID", currentResumeId);

                    // This clears the middle activities so 'Back' from Preview goes to the main list
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    startActivity(intent);
                    finish(); // Close the selection screen
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Error: Resume data lost", Toast.LENGTH_SHORT).show());
            }
        });
    }
}