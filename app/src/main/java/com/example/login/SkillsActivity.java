package com.example.login;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillsActivity extends BaseActivity {

    private RecyclerView rvSkills;
    private SkillAdapter adapter;
    private final List<String> skillList = new ArrayList<>(); // Stores "SkillName:Level"
    private AppDatabase db;
    private int resumeId;
    private Resume resume;

    private UserProfileManager profileManager;
    private boolean isProfileMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skills);

        db = AppDatabase.getInstance(this);
        resumeId = getIntent().getIntExtra("RESUME_ID", -1);
        isProfileMode = (resumeId == ProfileActivity.PROFILE_RESUME_ID);
        profileManager = new UserProfileManager();
        rvSkills = findViewById(R.id.rvSkills);
        rvSkills.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnUpgradeHint).setOnClickListener(v -> showSkillGuide());
        findViewById(R.id.btnAddSkill).setOnClickListener(v -> { if (adapter != null) adapter.addSkill(); });
        findViewById(R.id.btnSaveSkills).setOnClickListener(v -> saveSkills());

        loadData();
    }

    private void showSkillGuide() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        android.view.View sheetView = getLayoutInflater().inflate(R.layout.hint_bottom_sheet, null);
        android.widget.TextView hintText = sheetView.findViewById(R.id.hintContent);
        hintText.setText("💡 SKILLS TIPS:\n\n" +
                "• Beginner: Basic knowledge, still learning fundamentals.\n" +
                "• Intermediate: Can complete tasks but may need occasional help.\n" +
                "• Advanced: High proficiency, can solve complex problems.\n" +
                "• Expert: Deep mastery, can teach others or lead architecture.");
        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }

    private void loadData() {
        if (isProfileMode) { loadProfileSkills(); return; }
        new Thread(() -> {
            resume = db.resumeDao().getResumeById(resumeId);
            if (resume != null && resume.getSkills() != null) {
                skillList.addAll(Arrays.asList(resume.getSkills().split("\\|")));
            } else {
                skillList.add(":Beginner");
            }
            runOnUiThread(() -> {
                adapter = new SkillAdapter(skillList);
                rvSkills.setAdapter(adapter);
            });
        }).start();
    }

    private void loadProfileSkills() {
        profileManager.loadProfile(data -> runOnUiThread(() -> {
            Object v = data.get(UserProfileManager.KEY_SKILLS);
            String stored = v != null ? v.toString() : "";
            if (!stored.isEmpty()) {
                skillList.addAll(Arrays.asList(stored.split("\\|")));
            } else {
                skillList.add(":Beginner");
            }
            adapter = new SkillAdapter(skillList);
            rvSkills.setAdapter(adapter);
        }), err -> runOnUiThread(() -> {
            skillList.add(":Beginner");
            adapter = new SkillAdapter(skillList);
            rvSkills.setAdapter(adapter);
        }));
    }

    private void saveSkills() {
        if (adapter == null) return;
        StringBuilder sb = new StringBuilder();
        for (String s : adapter.getSkillsList()) {
            if (!s.startsWith(":")) sb.append(s).append("|");
        }
        if (isProfileMode) {
            profileManager.saveSkills(sb.toString(),
                    () -> runOnUiThread(() -> {
                        Toast.makeText(this, "Skills Saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    }),
                    err -> runOnUiThread(() ->
                            Toast.makeText(this, "Save failed: " + err, Toast.LENGTH_SHORT).show()));
            return;
        }
        new Thread(() -> {
            resume.setSkills(sb.toString());
            db.resumeDao().update(resume);
            runOnUiThread(() -> {
                Toast.makeText(this, "Skills Saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
