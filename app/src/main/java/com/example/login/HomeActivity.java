package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends BaseActivity {

    private MaterialCardView profileSection, resumeSection, oppSection;
    private MaterialCardView resumeCard, noResumeState;
    private LinearLayout aiSection, opportunitiesSection;
    private ImageView btnLanguage;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();

        // 1. Session Safety Check
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // 2. Initialize All Views
        btnLanguage          = findViewById(R.id.btn_language_menu);
        profileSection       = findViewById(R.id.profile_section);
        resumeSection        = findViewById(R.id.resume_section);
        oppSection           = findViewById(R.id.opp_section);
        aiSection            = findViewById(R.id.ai_section);
        opportunitiesSection = findViewById(R.id.opportunities_section);
        resumeCard           = findViewById(R.id.resume_card);
        noResumeState        = findViewById(R.id.no_resume_state);

        // 3. Show "Continue Building" card
        // Replace `true` with a real check if you have resume data
        boolean hasResume = true;
        if (resumeCard != null) {
            resumeCard.setVisibility(hasResume ? View.VISIBLE : View.GONE);
        }
        if (noResumeState != null) {
            noResumeState.setVisibility(hasResume ? View.GONE : View.VISIBLE);
        }

        // 4. Set up all button actions
        setupDashboardActions();
    }

    private void setupDashboardActions() {

        // --- Language Button ---
        if (btnLanguage != null) {
            btnLanguage.setOnClickListener(v -> showLanguageDialog());
        }

        // --- Spark AI Assistant ---
        if (aiSection != null) {
            aiSection.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, AiAssistantActivity.class)));
        }

        // --- Profile Section ---
        if (profileSection != null) {
            profileSection.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));
        }

        // --- Resume Builder ---
        if (resumeSection != null) {
            resumeSection.setOnClickListener(v ->
                    startActivity(new Intent(this, MyResumesActivity.class)));
        }

        // --- Opportunities Card ---
        if (oppSection != null) {
            oppSection.setOnClickListener(v ->
                    startActivity(new Intent(this, OpportunitiesActivity.class)));
        }

        // --- Opportunities Row (text link at bottom) ---
        if (opportunitiesSection != null) {
            opportunitiesSection.setOnClickListener(v ->
                    startActivity(new Intent(this, OpportunitiesActivity.class)));
        }

        // --- Resume Card (Continue Building) ---
        if (resumeCard != null) {
            resumeCard.setOnClickListener(v ->
                    startActivity(new Intent(this, MyResumesActivity.class)));
        }
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Русский"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_language))
                .setItems(languages, (dialog, which) -> {
                    if (which == 0) {
                        setAppLocale("en");
                    } else {
                        setAppLocale("ru");
                    }
                })
                .show();
    }

    private void setAppLocale(String languageCode) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }
}