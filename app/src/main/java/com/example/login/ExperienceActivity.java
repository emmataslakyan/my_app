package com.example.login;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExperienceActivity extends BaseActivity {

    private TextInputEditText editOrg, editPos, editLoc, editDate;
    private RecyclerView rvBullets;
    private BulletAdapter adapter;
    private final List<String> bulletList = new ArrayList<>();
    private AppDatabase db;
    private int resumeId;
    private Resume resume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experience);

        // 1. Initialize Database and Data
        db = AppDatabase.getInstance(this);
        resumeId = getIntent().getIntExtra("RESUME_ID", -1);

        // 2. Bind Views
        editOrg = findViewById(R.id.editOrgName);
        editPos = findViewById(R.id.editPosition);
        editLoc = findViewById(R.id.editExpLocation);
        editDate = findViewById(R.id.editExpDate);
        rvBullets = findViewById(R.id.rvBullets);

        // 3. Setup RecyclerView
        rvBullets.setLayoutManager(new LinearLayoutManager(this));

        // 4. Click Listeners
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        // This opens the Writing Guide
        findViewById(R.id.btnUpgradeHint).setOnClickListener(v -> showExperienceGuide());

        findViewById(R.id.btnAddBullet).setOnClickListener(v -> {
            if (adapter != null) adapter.addBullet();
        });

        findViewById(R.id.btnSaveExp).setOnClickListener(v -> saveExperience());

        loadData();
    }

    /**
     * Displays a BottomSheet with professional writing tips for the Experience section.
     */
    private void showExperienceGuide() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.hint_bottom_sheet, null);

        TextView hintText = sheetView.findViewById(R.id.hintContent);
        hintText.setText(
                "🚀 WORK EXPERIENCE TIPS:\n\n" +
                        "• Use Action Verbs: Start points with 'Managed', 'Created', or 'Improved'.\n" +
                        "• Quantify Results: Instead of 'Helped sales', use 'Increased sales by 20%'.\n" +
                        "• Reverse Order: Always list your most recent job first.\n" +
                        "• Focus on Impact: What was the result of your work? Mention tools used (e.g., Python, Figma)."
        );

        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }

    private void loadData() {
        if (resumeId == -1) return;

        new Thread(() -> {
            resume = db.resumeDao().getResumeById(resumeId);
            if (resume != null) {
                String savedBullets = resume.getExpBullets();
                bulletList.clear(); // Clear to avoid duplicates on reload
                if (savedBullets != null && !savedBullets.isEmpty()) {
                    String[] points = savedBullets.split("\\|");
                    bulletList.addAll(Arrays.asList(points));
                } else {
                    bulletList.add(""); // Start with one empty line
                }

                runOnUiThread(() -> {
                    editOrg.setText(resume.getExpOrgName());
                    editPos.setText(resume.getExpPosition());
                    editLoc.setText(resume.getExpLocation());
                    editDate.setText(resume.getExpDate());

                    adapter = new BulletAdapter(bulletList);
                    rvBullets.setAdapter(adapter);
                });
            }
        }).start();
    }

    private void saveExperience() {
        if (resume == null) return;

        // Extracting text safely
        String org = editOrg.getText() != null ? editOrg.getText().toString() : "";
        String pos = editPos.getText() != null ? editPos.getText().toString() : "";
        String loc = editLoc.getText() != null ? editLoc.getText().toString() : "";
        String date = editDate.getText() != null ? editDate.getText().toString() : "";

        resume.setExpOrgName(org);
        resume.setExpPosition(pos);
        resume.setExpLocation(loc);
        resume.setExpDate(date);

        // Join bullets into a single pipe-separated string
        StringBuilder sb = new StringBuilder();
        if (adapter != null) {
            for (String s : adapter.getBullets()) {
                if (s != null && !s.trim().isEmpty()) {
                    sb.append(s).append("|");
                }
            }
        }
        resume.setExpBullets(sb.toString());

        new Thread(() -> {
            db.resumeDao().update(resume);
            runOnUiThread(() -> {
                Toast.makeText(this, "Experience Saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}