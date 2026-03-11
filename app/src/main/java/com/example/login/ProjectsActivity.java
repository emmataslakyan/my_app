package com.example.login;

import android.os.Bundle;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;

public class ProjectsActivity extends BaseActivity {

    private int currentResumeId = -1;
    private TextInputEditText etProjectName, etRole, etBulletPoints;
    private TextInputEditText etStartMonth, etStartYear, etEndMonth, etEndYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

        // Retrieve ID from intent
        currentResumeId = getIntent().getIntExtra("RESUME_ID", -1);

        // Initialize Views
        etProjectName = findViewById(R.id.etProjectName);
        etRole = findViewById(R.id.etRole);
        etStartMonth = findViewById(R.id.etStartMonth);
        etStartYear = findViewById(R.id.etStartYear);
        etEndMonth = findViewById(R.id.etEndMonth);
        etEndYear = findViewById(R.id.etEndYear);
        etBulletPoints = findViewById(R.id.etBulletPoints);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveProject).setOnClickListener(v -> saveProjectData());

        // Optional: Load existing data if editing
        loadExistingData();
    }

    private void saveProjectData() {
        String name = etProjectName.getText().toString().trim();
        String role = etRole.getText().toString().trim();
        String start = etStartMonth.getText().toString().trim() + " " + etStartYear.getText().toString().trim();
        String end = etEndMonth.getText().toString().trim() + " " + etEndYear.getText().toString().trim();
        String bullets = etBulletPoints.getText().toString().trim();

        if (name.isEmpty()) {
            etProjectName.setError("Project name is required");
            return;
        }

        // Save to SharedPreferences (or your SQLite DB)
        getSharedPreferences("ResumeData_" + currentResumeId, MODE_PRIVATE)
                .edit()
                .putString("project_name", name)
                .putString("project_role", role)
                .putString("project_start", start)
                .putString("project_end", end)
                .putString("project_bullets", bullets)
                .apply();

        Toast.makeText(this, "Project saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void loadExistingData() {
        // This ensures if the user re-opens the page, their data is still there
        var prefs = getSharedPreferences("ResumeData_" + currentResumeId, MODE_PRIVATE);
        etProjectName.setText(prefs.getString("project_name", ""));
        etRole.setText(prefs.getString("project_role", ""));
        etBulletPoints.setText(prefs.getString("project_bullets", ""));
        // Note: For dates, you'd typically split the saved string back into Month/Year
    }
}