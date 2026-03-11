package com.example.login;

import android.os.Bundle;
import android.widget.TextView;
import java.util.concurrent.Executors;

public class ResumeViewActivity extends BaseActivity {

    private int resumeId;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume_view);

        resumeId = getIntent().getIntExtra("RESUME_ID", -1);
        db = AppDatabase.getInstance(this);

        loadResumeData();
    }

    private void loadResumeData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Resume resume = db.resumeDao().getResumeById(resumeId);

            if (resume != null) {
                runOnUiThread(() -> {
                    // Fill UI with data from Database
                    ((TextView) findViewById(R.id.tvFullName)).setText(resume.getName());
                    ((TextView) findViewById(R.id.tvContactInfo)).setText(
                            resume.getEmail() + " | " + resume.getPhone() + "\n" + resume.getAddress()
                    );

                    ((TextView) findViewById(R.id.tvEducationDetails)).setText(
                            resume.getSchoolName() + " (" + resume.getSchoolDate() + ")\n" + resume.getDegree()
                    );

                    ((TextView) findViewById(R.id.tvExperienceDetails)).setText(
                            resume.getExpOrgName() + " - " + resume.getExpPosition() + "\n" + resume.getExpBullets()
                    );

                    ((TextView) findViewById(R.id.tvSkills)).setText(resume.getSkills());
                });
            }
        });
    }
}