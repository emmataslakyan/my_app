package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends BaseActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        // 1. Display User Info
        TextView profileUsername = findViewById(R.id.profile_username);
        TextView profileEmail = findViewById(R.id.profile_email);
        if (user != null) {
            profileUsername.setText(user.getDisplayName() != null ? user.getDisplayName() : "User Name");
            profileEmail.setText(user.getEmail());
        }

        // 2. Setup Toolbar (Back and Language)
        setupCustomToolbar();

        // 3. Setup Resume Section Cards
        setupSectionCards();

        // 4. Logout Button Logic
        Button logoutBtn = findViewById(R.id.logout_button);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> performLogout());
        }

        // 5. Bottom Bar Logic
        LinearLayout bottomBar = findViewById(R.id.bottomBar);
        if (bottomBar != null) {
            bottomBar.setOnClickListener(v -> {
                Toast.makeText(this, "Opening Resume Preview...", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupCustomToolbar() {
        ImageView btnBack = findViewById(R.id.btn_back);
        ImageView btnLang = findViewById(R.id.btn_language_menu);

        // Back to Home
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, HomeActivity.class));
                finish();
            });
        }

        // Language Switcher (Uses your modern sheet logic)
        if (btnLang != null) {
            btnLang.setOnClickListener(v -> showModernLanguageSheet());
        }
    }

    private void performLogout() {
        mAuth.signOut();
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showModernLanguageSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_language_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        sheetView.findViewById(R.id.btn_select_en).setOnClickListener(v -> {
            setAppLocale("en");
            bottomSheetDialog.dismiss();
        });

        sheetView.findViewById(R.id.btn_select_ru).setOnClickListener(v -> {
            setAppLocale("ru");
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void setAppLocale(String languageCode) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }

    private void setupSectionCards() {
        int[] cardIds = {
                R.id.btnPersonalDetails, R.id.btnEducation, R.id.btnExperience,
                R.id.btnVolunteering, R.id.btnSkills, R.id.btnLanguages,
                R.id.btnProjects, R.id.btnAddMore
        };

        for (int id : cardIds) {
            CardView card = findViewById(id);
            if (card != null) {
                card.setOnClickListener(v -> {
                    // Navigate to Editor for any section click
                    startActivity(new Intent(this, ResumeEditorActivity.class));
                });
            }
        }
    }
}