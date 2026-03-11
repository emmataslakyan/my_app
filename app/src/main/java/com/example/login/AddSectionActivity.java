package com.example.login;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

public class AddSectionActivity extends BaseActivity {

    // 1. Declare the variable at the class level
    private int resumeId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_section);

        // 2. Initialize it from the Intent
        resumeId = getIntent().getIntExtra("RESUME_ID", -1);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        // Setup Toggles
        setupToggle(R.id.cardProjects, R.id.checkProjects, "Projects");
        setupToggle(R.id.cardAwards, R.id.checkAwards, "Honors & Awards");

        findViewById(R.id.btnCreateCustom).setOnClickListener(v -> showCustomDialog());
    }

    private void setupToggle(int cardId, int checkId, String name) {
        CardView card = findViewById(cardId);
        CheckBox checkBox = findViewById(checkId);

        // 3. Use the lowercase 'resumeId' here
        boolean isEnabled = getSharedPreferences("Sections_" + resumeId, MODE_PRIVATE)
                .getBoolean(name, false);
        checkBox.setChecked(isEnabled);

        card.setOnClickListener(v -> checkBox.setChecked(!checkBox.isChecked()));

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the state using 'resumeId'
            getSharedPreferences("Sections_" + resumeId, MODE_PRIVATE)
                    .edit()
                    .putBoolean(name, isChecked)
                    .apply();

            if (isChecked) {
                Toast.makeText(this, name + " added", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCustomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Name your Section");

        final EditText input = new EditText(this);
        input.setHint("e.g. Research, References");

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);

        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String sectionName = input.getText().toString();
            if (!sectionName.isEmpty()) {
                // To make custom sections work, we'd save the name to a list in SharedPreferences
                saveCustomSection(sectionName);
                Toast.makeText(this, sectionName + " Created!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveCustomSection(String name) {
        // Logic to store custom section names can go here
    }
}