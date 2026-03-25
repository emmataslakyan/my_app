package com.example.login;

import android.os.Bundle;
import android.widget.TextView;
import java.util.concurrent.Executors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.concurrent.Executors;

public class ResumeViewActivity extends BaseActivity {

    private int resumeId;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure your XML file is named activity_resume_view.xml
        setContentView(R.layout.activity_resume_view);

        resumeId = getIntent().getIntExtra("RESUME_ID", -1);
        db = AppDatabase.getInstance(this);

        // Optional: If you have a back button in this layout
        View backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        loadResumeData();
    }

    private void loadResumeData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Resume resume = db.resumeDao().getResumeById(resumeId);

            if (resume != null) {
                // Switch back to the main thread to update the UI
                runOnUiThread(() -> populateUI(resume));
            }
        });
    }

    private void populateUI(Resume resume) {
        // 1. Header & Contact Line (Formatted: Location | Email | Phone)
        setTextIfNotNull(R.id.previewName, resume.getName());

        StringBuilder contact = new StringBuilder();
        if (resume.getAddress() != null) contact.append(resume.getAddress());
        if (resume.getEmail() != null) contact.append(" | ").append(resume.getEmail());
        if (resume.getPhone() != null) contact.append(" | ").append(resume.getPhone());
        setTextIfNotNull(R.id.previewContactInfo, contact.toString());

        // 2. Setup dynamic containers (Matching the XML IDs below)
        LinearLayout eduContainer = findViewById(R.id.sectionEducation);
        LinearLayout expContainer = findViewById(R.id.sectionExperience);

        if (eduContainer != null) {
            // We keep the header/divider and only manage the content area
            // Or use the static mapping if you aren't using a list of multiple items
            setTextIfNotNull(R.id.previewEduName, resume.getSchoolName());
            setTextIfNotNull(R.id.previewEduLocation, resume.getSchoolLocation());
            setTextIfNotNull(R.id.previewEduDesc, resume.getSchoolDescription());
        }

        if (expContainer != null) {
            setTextIfNotNull(R.id.previewExpName, resume.getExpOrgName());
            setTextIfNotNull(R.id.previewExpDesc, resume.getExpBullets());
        }

        setTextIfNotNull(R.id.previewSkills, resume.getSkills());
    }
    private void addEntryToContainer(LinearLayout container, String head, String sub, String desc) {
        // If there is no data or container, do nothing
        if (container == null || head == null || head.isEmpty()) return;

        View itemView = LayoutInflater.from(this).inflate(R.layout.item_resume_entry, container, false);

        TextView tvHead = itemView.findViewById(R.id.itemHeader);
        TextView tvSub = itemView.findViewById(R.id.itemSubHeader);
        TextView tvDesc = itemView.findViewById(R.id.itemDescription);

        tvHead.setText(head);
        tvSub.setText(sub != null ? sub : "");
        tvDesc.setText(desc != null ? desc : "");

        // If there is no description, hide the TextView so it doesn't take up empty space
        if (desc == null || desc.trim().isEmpty()) {
            tvDesc.setVisibility(View.GONE);
        }

        container.addView(itemView);
    }

    // Helper method to safely set text
    private void setTextIfNotNull(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null) {
            tv.setText(text != null ? text : "");
        }
    }

    // Helper method to build the contact line without awkward nulls
    private String formatContactInfo(String email, String phone, String address) {
        StringBuilder sb = new StringBuilder();
        if (email != null && !email.isEmpty()) sb.append(email);
        if (phone != null && !phone.isEmpty()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(phone);
        }
        if (address != null && !address.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(address);
        }
        return sb.toString();
    }
}