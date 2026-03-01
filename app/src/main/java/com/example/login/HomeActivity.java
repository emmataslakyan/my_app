package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends BaseActivity {

    private LinearLayout profileSection, resumeSection, aiSection, opportunitiesSection;
    private ShapeableImageView btnLanguage;
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
        btnLanguage = findViewById(R.id.btn_language_menu);
        profileSection = findViewById(R.id.profile_section);
        resumeSection = findViewById(R.id.resume_section);
        aiSection = findViewById(R.id.ai_section);
        opportunitiesSection = findViewById(R.id.opportunities_section);

        // 3. Set up all button actions
        setupDashboardActions();
    }

    private void setupDashboardActions() {
        // --- Language Button ---
        if (btnLanguage != null) {
            btnLanguage.setOnClickListener(v -> showLanguageDialog());
        }

        // --- Spark AI Assistant (RE-ENABLED) ---
        if (aiSection != null) {
            aiSection.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, AiAssistantActivity.class);
                startActivity(intent);
            });
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

        // --- Opportunities Section ---
        if (opportunitiesSection != null) {
            opportunitiesSection.setOnClickListener(v ->
                    Toast.makeText(this, R.string.menu_opps, Toast.LENGTH_SHORT).show());
        }
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Русский"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // FIXED: Now using the specific ID for languages
        builder.setTitle(getString(R.string.select_language));

        builder.setItems(languages, (dialog, which) -> {
            if (which == 0) {
                setAppLocale("en");
            } else {
                setAppLocale("ru");
            }
        });
        builder.show();
    }

    // This is the "Magic Fix" for translation.
    // It tells the whole app to switch resources and refreshes the UI automatically.
    private void setAppLocale(String languageCode) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }
}