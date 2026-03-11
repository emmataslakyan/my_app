package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

public class ResumeEditorActivity extends BaseActivity {

    private int currentResumeId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume_editor);

        currentResumeId = getIntent().getIntExtra("RESUME_ID", -1);

        // 1. Navigation Listeners (All sections)
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        findViewById(R.id.btnPersonalDetails).setOnClickListener(v -> openSection(PersonalDetailsActivity.class));
        findViewById(R.id.btnEducation).setOnClickListener(v -> openSection(EducationActivity.class));
        findViewById(R.id.btnExperience).setOnClickListener(v -> openSection(ExperienceActivity.class));
        findViewById(R.id.btnSkills).setOnClickListener(v -> openSection(SkillsActivity.class));
        findViewById(R.id.btnVolunteering).setOnClickListener(v -> openSection(VolunteeringActivity.class));
        findViewById(R.id.btnLanguages).setOnClickListener(v -> openSection(LanguagesActivity.class));

        // Dynamic Sections
        findViewById(R.id.btnProjects).setOnClickListener(v -> openSection(ProjectsActivity.class));
        findViewById(R.id.btnAwards).setOnClickListener(v -> {
            // If you have an AwardsActivity, open it here
            // openSection(AwardsActivity.class);
        });
        // Bottom Bar Listener to choose template
        findViewById(R.id.bottomBar).setOnClickListener(v -> {
            Intent intent = new Intent(this, TemplateSelectionActivity.class);
            intent.putExtra("RESUME_ID", currentResumeId);
            startActivity(intent);
        });

        // Add More & Rename
        findViewById(R.id.btnAddMore).setOnClickListener(v -> openSection(AddSectionActivity.class));
        findViewById(R.id.btnEditSectionNames).setOnClickListener(v -> showRenameMenu(v));

        loadCustomNames();
    }


    private void showRenameMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Rename Personal Details");
        popup.getMenu().add("Rename Education");
        popup.getMenu().add("Rename Experience");
        popup.getMenu().add("Rename Skills");
        popup.getMenu().add("Rename Volunteering");
        popup.getMenu().add("Rename Languages");
        popup.getMenu().add("Rename Projects");
        popup.getMenu().add("Rename Honors & Awards");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.contains("Personal")) showRenameDialog(findViewById(R.id.tvPersonalTitle), "name_personal");
            else if (title.contains("Education")) showRenameDialog(findViewById(R.id.tvEducationTitle), "name_edu");
            else if (title.contains("Experience")) showRenameDialog(findViewById(R.id.tvExperienceTitle), "name_exp");
            else if (title.contains("Skills")) showRenameDialog(findViewById(R.id.tvSkillsTitle), "name_skills");
            else if (title.contains("Volunteering")) showRenameDialog(findViewById(R.id.tvVolunteeringTitle), "name_vol");
            else if (title.contains("Languages")) showRenameDialog(findViewById(R.id.tvLanguagesTitle), "name_lang");
            else if (title.contains("Projects")) showRenameDialog(findViewById(R.id.tvProjectsTitle), "name_projects");
            else if (title.contains("Awards")) showRenameDialog(findViewById(R.id.tvAwardsTitle), "name_awards");
            return true;
        });
        popup.show();
    }

    private void showRenameDialog(TextView targetTextView, String prefKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Section");

        final EditText input = new EditText(this);
        input.setText(targetTextView.getText().toString());

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 60; params.rightMargin = 60; // Better spacing
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                targetTextView.setText(newName);
                getSharedPreferences("CustomNames_" + currentResumeId, MODE_PRIVATE)
                        .edit().putString(prefKey, newName).apply();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadCustomNames() {
        var prefs = getSharedPreferences("CustomNames_" + currentResumeId, MODE_PRIVATE);

        ((TextView)findViewById(R.id.tvPersonalTitle)).setText(prefs.getString("name_personal", "Personal Details"));
        ((TextView)findViewById(R.id.tvEducationTitle)).setText(prefs.getString("name_edu", "Education"));
        ((TextView)findViewById(R.id.tvExperienceTitle)).setText(prefs.getString("name_exp", "Experience"));
        ((TextView)findViewById(R.id.tvSkillsTitle)).setText(prefs.getString("name_skills", "Skills"));
        ((TextView)findViewById(R.id.tvVolunteeringTitle)).setText(prefs.getString("name_vol", "Volunteering"));
        ((TextView)findViewById(R.id.tvLanguagesTitle)).setText(prefs.getString("name_lang", "Languages"));
        ((TextView)findViewById(R.id.tvProjectsTitle)).setText(prefs.getString("name_projects", "Projects"));
        ((TextView)findViewById(R.id.tvAwardsTitle)).setText(prefs.getString("name_awards", "Honors & Awards"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        String secPrefs = "Sections_" + currentResumeId;
        boolean showProjects = getSharedPreferences(secPrefs, MODE_PRIVATE).getBoolean("Projects", false);
        boolean showAwards = getSharedPreferences(secPrefs, MODE_PRIVATE).getBoolean("Honors & Awards", false);

        findViewById(R.id.btnProjects).setVisibility(showProjects ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnAwards).setVisibility(showAwards ? View.VISIBLE : View.GONE);

        loadCustomNames();
    }

    private void openSection(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.putExtra("RESUME_ID", currentResumeId);
        startActivity(intent);
    }
}