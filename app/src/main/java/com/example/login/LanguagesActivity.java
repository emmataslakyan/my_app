package com.example.login;

import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LanguagesActivity extends BaseActivity {

    private RecyclerView rvLanguages;
    private LanguageAdapter adapter;
    private final List<String> langList = new ArrayList<>(); // "Language:Proficiency"
    private ResumeRepository repo;
    private String resumeId;
    private Resume resume;

    private UserProfileManager profileManager;
    private boolean isProfileMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_languages);

        repo = new ResumeRepository();
        resumeId = getIntent().getStringExtra("RESUME_ID");
        isProfileMode = ProfileActivity.PROFILE_RESUME_ID.equals(resumeId);
        profileManager = new UserProfileManager();

        rvLanguages = findViewById(R.id.rvLanguages);
        rvLanguages.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnUpgradeHint).setOnClickListener(v -> showLanguageGuide());
        findViewById(R.id.btnAddLanguage).setOnClickListener(v -> { if (adapter != null) adapter.addLanguage(); });
        findViewById(R.id.btnSaveLanguages).setOnClickListener(v -> saveLanguages());

        loadData();
    }

    private void showLanguageGuide() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        android.view.View sheetView = getLayoutInflater().inflate(R.layout.hint_bottom_sheet, null);
        android.widget.TextView hint = sheetView.findViewById(R.id.hintContent);
        hint.setText(
                "🌍 LANGUAGE TIPS:\n\n" +
                "• Native: Your mother tongue or equivalent fluency.\n" +
                "• Fluent: Can communicate naturally in most situations.\n" +
                "• Advanced: Strong command, occasional mistakes.\n" +
                "• Intermediate: Can handle everyday conversations.\n" +
                "• Basic: Limited vocabulary, simple phrases only."
        );
        sheet.setContentView(sheetView);
        sheet.show();
    }

    private void loadData() {
        if (isProfileMode) { loadProfileLanguages(); return; }
        if (resumeId == null || resumeId.isEmpty()) return;
        repo.get(resumeId, r -> {
            resume = r;
            if (r.getLanguages() != null && !r.getLanguages().isEmpty()) {
                langList.addAll(Arrays.asList(r.getLanguages().split("\\|")));
            } else {
                langList.add(":Fluent");
            }
            adapter = new LanguageAdapter(langList);
            rvLanguages.setAdapter(adapter);
        }, err -> Toast.makeText(this, "Couldn't load resume: " + err, Toast.LENGTH_SHORT).show());
    }

    private void loadProfileLanguages() {
        profileManager.loadProfile(data -> runOnUiThread(() -> {
            Object v = data.get(UserProfileManager.KEY_LANGUAGES);
            String stored = v != null ? v.toString() : "";
            if (!stored.isEmpty()) {
                langList.addAll(Arrays.asList(stored.split("\\|")));
            } else {
                langList.add(":Fluent");
            }
            adapter = new LanguageAdapter(langList);
            rvLanguages.setAdapter(adapter);
        }), err -> runOnUiThread(() -> {
            langList.add(":Fluent");
            adapter = new LanguageAdapter(langList);
            rvLanguages.setAdapter(adapter);
        }));
    }

    private void saveLanguages() {
        if (adapter == null) return;
        StringBuilder sb = new StringBuilder();
        for (String s : adapter.getLangList()) {
            if (!s.startsWith(":")) sb.append(s).append("|");
        }
        if (isProfileMode) {
            profileManager.saveLanguages(sb.toString(),
                    () -> runOnUiThread(() -> {
                        Toast.makeText(this, "Languages Saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    }),
                    err -> runOnUiThread(() ->
                            Toast.makeText(this, "Save failed: " + err, Toast.LENGTH_SHORT).show()));
            return;
        }
        if (resume == null) return;
        resume.setLanguages(sb.toString());
        repo.update(resume,
                () -> {
                    Toast.makeText(this, "Languages Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                err -> Toast.makeText(this, "Save failed: " + err, Toast.LENGTH_SHORT).show());
    }
}
