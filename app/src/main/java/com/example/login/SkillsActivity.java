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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skills);

        db = AppDatabase.getInstance(this);
        resumeId = getIntent().getIntExtra("RESUME_ID", -1);
        rvSkills = findViewById(R.id.rvSkills);
        rvSkills.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnUpgradeHint).setOnClickListener(v -> showSkillGuide());
        findViewById(R.id.btnAddSkill).setOnClickListener(v -> adapter.addSkill());
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
        new Thread(() -> {
            resume = db.resumeDao().getResumeById(resumeId);
            if (resume != null && resume.getSkills() != null) {
                skillList.addAll(Arrays.asList(resume.getSkills().split("\\|")));
            } else {
                skillList.add(":Beginner"); // Default empty item
            }
            runOnUiThread(() -> {
                adapter = new SkillAdapter(skillList);
                rvSkills.setAdapter(adapter);
            });
        }).start();
    }

    private void saveSkills() {
        StringBuilder sb = new StringBuilder();
        for (String s : adapter.getSkillsList()) {
            if (!s.startsWith(":")) sb.append(s).append("|");
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
