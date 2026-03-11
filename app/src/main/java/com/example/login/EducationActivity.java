package com.example.login;

import android.os.Bundle;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EducationActivity extends BaseActivity {

    private TextInputEditText editSchool, editLocation, editDate, editDegree;
    private RecyclerView rvBullets;
    private BulletAdapter adapter;
    private final List<String> bulletList = new ArrayList<>();

    private AppDatabase db;
    private int currentResumeId;
    private Resume currentResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education);

        db = AppDatabase.getInstance(this);
        currentResumeId = getIntent().getIntExtra("RESUME_ID", -1);

        // Initialize Standard Inputs
        editSchool = findViewById(R.id.editSchool);
        editLocation = findViewById(R.id.editSchoolLocation);
        editDate = findViewById(R.id.editSchoolDate);
        editDegree = findViewById(R.id.editDegree);

        // Setup RecyclerView for Bullet Points
        rvBullets = findViewById(R.id.rvEducationBullets);
        rvBullets.setLayoutManager(new LinearLayoutManager(this));

        // Button Listeners
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        findViewById(R.id.btnUpgradeHint).setOnClickListener(v -> showEducationGuide());

        findViewById(R.id.btnAddEduBullet).setOnClickListener(v -> {
            if (adapter != null) adapter.addBullet();
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> saveEducation());

        loadData();
    }

    private void showEducationGuide() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        android.view.View sheetView = getLayoutInflater().inflate(R.layout.hint_bottom_sheet, null);

        android.widget.TextView hintText = sheetView.findViewById(R.id.hintContent);
        hintText.setText(
                "🎓 EDUCATION TIPS:\n\n" +
                        "• GPA: Only include if it is 3.5 or higher.\n" +
                        "• Relevant Coursework: List classes that match the job description.\n" +
                        "• Honors: Mention Dean's List, scholarships, or club leadership.\n" +
                        "• Study Abroad: If applicable, list the city and university."
        );

        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }

    private void loadData() {
        if (currentResumeId == -1) return;

        new Thread(() -> {
            currentResume = db.resumeDao().getResumeById(currentResumeId);
            if (currentResume != null) {
                // Split the saved pipe-separated string into the list
                String savedActivities = currentResume.getSchoolDescription();
                if (savedActivities != null && !savedActivities.isEmpty()) {
                    String[] points = savedActivities.split("\\|");
                    bulletList.addAll(Arrays.asList(points));
                } else {
                    bulletList.add(""); // Default empty line
                }

                runOnUiThread(() -> {
                    editSchool.setText(currentResume.getSchoolName());
                    editLocation.setText(currentResume.getSchoolLocation());
                    editDate.setText(currentResume.getSchoolDate());
                    editDegree.setText(currentResume.getDegree());

                    adapter = new BulletAdapter(bulletList);
                    rvBullets.setAdapter(adapter);
                });
            }
        }).start();
    }

    private void saveEducation() {
        if (currentResume == null) return;

        currentResume.setSchoolName(String.valueOf(editSchool.getText()));
        currentResume.setSchoolLocation(String.valueOf(editLocation.getText()));
        currentResume.setSchoolDate(String.valueOf(editDate.getText()));
        currentResume.setDegree(String.valueOf(editDegree.getText()));

        // Join the list into a single string: "GPA 3.9|Math Club|Dean's List"
        StringBuilder sb = new StringBuilder();
        if (adapter != null) {
            for (String s : adapter.getBullets()) {
                if (s != null && !s.trim().isEmpty()) {
                    sb.append(s).append("|");
                }
            }
        }
        currentResume.setSchoolDescription(sb.toString());

        new Thread(() -> {
            db.resumeDao().update(currentResume);
            runOnUiThread(() -> {
                Toast.makeText(this, "Education Saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}