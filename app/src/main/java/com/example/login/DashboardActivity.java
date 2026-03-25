package com.example.login;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class DashboardActivity extends BaseActivity {

    // ── Action / nav views ───────────────────────
    private ImageView        btnLanguageMenu;
    private LinearLayout     aiSection;
    private LinearLayout     opportunitiesSection;
    private MaterialCardView resumeSection;
    private MaterialCardView profileSection;
    private MaterialCardView tilePeer;
    private MaterialCardView tileFinancial;
    private MaterialCardView tileProximity;
    private MaterialCardView btnMergeLayout;
    private MaterialCardView btnSplitLayout;
    private MaterialCardView btnCompressLayout;
    private MaterialCardView btnImgToPdfLayout;

    // ── Resume hero card ─────────────────────────
    private MaterialCardView resumeCard;
    private MaterialCardView noResumeState;
    private MaterialButton   btnViewResume;
    private TextView         resumeCardTitle;
    private TextView         resumeCardDays;
    private ProgressBar      resumeProgressBar;

    // ── Gradient target TextViews ────────────────
    private TextView dashboardTitle;
    private TextView continueLabel;
    private TextView pdfToolsLabel;
    private TextView sparkAssistantText;
    private TextView discoverLabel;

    // ─────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bindViews();
        applyGradients();
        setupClickListeners();
        loadResumeState();
    }

    // ─────────────────────────────────────────────
    // BIND VIEWS
    // ─────────────────────────────────────────────

    private void bindViews() {
        // Action / nav
        btnLanguageMenu      = findViewById(R.id.btn_language_menu);
        aiSection            = findViewById(R.id.ai_section);
        opportunitiesSection = findViewById(R.id.opportunities_section);
        resumeSection        = findViewById(R.id.resume_section);
        profileSection       = findViewById(R.id.profile_section);
        tilePeer             = findViewById(R.id.tile_peer);


        // Resume hero card
        resumeCard        = findViewById(R.id.resume_card);
        noResumeState     = findViewById(R.id.no_resume_state);
        btnViewResume     = findViewById(R.id.btn_view_resume);
        resumeCardTitle   = findViewById(R.id.resume_card_title);
        resumeCardDays    = findViewById(R.id.resume_card_days);
        resumeProgressBar = findViewById(R.id.resume_progress_bar);

        // Gradient TextViews
        dashboardTitle     = findViewById(R.id.dashboard_title);
        continueLabel      = findViewById(R.id.continueLabel);

        sparkAssistantText = findViewById(R.id.sparkAssistantText);
        discoverLabel      = findViewById(R.id.discoverLabel);
    }

    // ─────────────────────────────────────────────
    // GRADIENT TEXT
    // ─────────────────────────────────────────────

    private void applyGradients() {
        if (dashboardTitle != null)
            GradientTextUtils.applyGradient(dashboardTitle,
                    Color.parseColor("#9B5DFF"),
                    Color.parseColor("#280F4F"));

        if (continueLabel != null)
            GradientTextUtils.applyGradient(continueLabel,
                    Color.parseColor("#051256"),
                    Color.parseColor("#46287A"));

        if (pdfToolsLabel != null)
            GradientTextUtils.applyGradient(pdfToolsLabel,
                    Color.parseColor("#46287A"),
                    Color.parseColor("#9B5DFF"));

        if (sparkAssistantText != null)
            GradientTextUtils.applyGradient(sparkAssistantText,
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#BBA8DE"));

        if (discoverLabel != null)
            GradientTextUtils.applyGradient(discoverLabel,
                    Color.parseColor("#051256"),
                    Color.parseColor("#7B2FF7"));
    }

    // ─────────────────────────────────────────────
    // RESUME STATE
    // ─────────────────────────────────────────────

    private void loadResumeState() {
        if (resumeCard == null || noResumeState == null) return;

        boolean hasDraft = false; // TODO: connect to Room / ViewModel

        if (hasDraft) {
            resumeCard.setVisibility(View.VISIBLE);
            noResumeState.setVisibility(View.GONE);

            if (resumeCardTitle != null) {
                resumeCardTitle.setText("Active Resume Draft");
                GradientTextUtils.applyGradient(resumeCardTitle,
                        Color.parseColor("#280F4F"),
                        Color.parseColor("#46287A"));
            }
            if (resumeCardDays    != null) resumeCardDays.setText("Progress – 12 days");
            if (resumeProgressBar != null) resumeProgressBar.setProgress(40);

        } else {
            resumeCard.setVisibility(View.GONE);
            noResumeState.setVisibility(View.VISIBLE);
        }
    }

    // ─────────────────────────────────────────────
    // CLICK LISTENERS
    // ─────────────────────────────────────────────

    private void setupClickListeners() {
        if (btnLanguageMenu != null)
            btnLanguageMenu.setOnClickListener(v -> showLanguageDialog());

        if (aiSection != null)
            aiSection.setOnClickListener(v ->
                    startActivity(new Intent(this, AiAssistantActivity.class)));

        if (resumeSection != null)
            resumeSection.setOnClickListener(v ->
                    startActivity(new Intent(this, MyResumesActivity.class)));

        if (btnViewResume != null)
            btnViewResume.setOnClickListener(v ->
                    startActivity(new Intent(this, MyResumesActivity.class)));

        if (profileSection != null)
            profileSection.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));

        if (opportunitiesSection != null)
            opportunitiesSection.setOnClickListener(v ->
                    Toast.makeText(this, R.string.menu_opps, Toast.LENGTH_SHORT).show());

        if (tilePeer != null)
            tilePeer.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));

        if (tileFinancial != null)
            tileFinancial.setOnClickListener(v ->
                    startActivity(new Intent(this, MyResumesActivity.class)));

        if (tileProximity != null)
            tileProximity.setOnClickListener(v ->
                    Toast.makeText(this, "Proximity Sectors coming soon", Toast.LENGTH_SHORT).show());

        if (btnMergeLayout != null)
            btnMergeLayout.setOnClickListener(v ->
                    startActivity(new Intent(this, MergePdfActivity.class)));

        if (btnSplitLayout != null)
            btnSplitLayout.setOnClickListener(v ->
                    startActivity(new Intent(this, SplitPdfActivity.class)));

        if (btnCompressLayout != null)
            btnCompressLayout.setOnClickListener(v ->
                    startActivity(new Intent(this, CompressPdfActivity.class)));

        if (btnImgToPdfLayout != null)
            btnImgToPdfLayout.setOnClickListener(v ->
                    startActivity(new Intent(this, ImgToPdfActivity.class)));
    }

    // ─────────────────────────────────────────────
    // LANGUAGE DIALOG
    // ─────────────────────────────────────────────

    private void showLanguageDialog() {
        String[] languages = {"English", "Русский"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_language))
                .setItems(languages, (dialog, which) ->
                        setAppLocale(which == 0 ? "en" : "ru"))
                .show();
    }

    private void setAppLocale(String languageCode) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }
}