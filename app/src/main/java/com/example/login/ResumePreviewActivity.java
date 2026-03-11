package com.example.login;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ResumePreviewActivity extends BaseActivity {

    private int currentResumeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume_preview);

        currentResumeId = getIntent().getIntExtra("RESUME_ID", -1);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        loadResumeData();
    }

    private void loadResumeData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Resume resume = db.resumeDao().getResumeById(currentResumeId);

            if (resume != null) {
                runOnUiThread(() -> populateUI(resume));
            }
        }).start();
    }

    private void populateUI(Resume resume) {
        // 1. Header & Contact Line
        updateText(R.id.previewName, resume.getName());
        String contact = (resume.getAddress() != null ? resume.getAddress() : "") + " | " +
                (resume.getEmail() != null ? resume.getEmail() : "") + " | " +
                (resume.getPhone() != null ? resume.getPhone() : "");
        updateText(R.id.previewContactInfo, contact);

        // 2. Section Headers (Localized/Custom Names)
        var prefs = getSharedPreferences("CustomNames_" + currentResumeId, MODE_PRIVATE);
        updateText(R.id.labelEducation, prefs.getString("name_edu", "FORMAL EDUCATION").toUpperCase());
        updateText(R.id.labelExperience, prefs.getString("name_exp", "EXPERIENCE").toUpperCase());

        // 3. Education Content
        updateText(R.id.previewEduName, resume.getSchoolName());
        updateText(R.id.previewEduLocation, resume.getSchoolLocation());
        updateText(R.id.previewEduDesc, resume.getSchoolDescription());

        // 4. Experience Content
        updateText(R.id.previewExpName, resume.getExpOrgName());
        updateText(R.id.previewExpDesc, resume.getExpBullets());

        // 5. Section Visibility
        toggleSection(R.id.sectionEducation, resume.getSchoolName());
        toggleSection(R.id.sectionExperience, resume.getExpOrgName());
    }

    // These two methods should appear ONLY ONCE at the bottom of your Activity class
    private void updateText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null) {
            tv.setText(text != null ? text : "");
        }
    }

    private void toggleSection(int sectionId, String data) {
        View section = findViewById(sectionId);
        if (section != null) {
            section.setVisibility(data == null || data.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
}