package com.example.login;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

public class PersonalDetailsActivity extends BaseActivity {

    private EditText editName, editEmail, editPhone, editAddress;
    private AppDatabase db;
    private int currentResumeId;
    private Resume currentResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        db = AppDatabase.getInstance(this);
        currentResumeId = getIntent().getIntExtra("RESUME_ID", -1);

        // Initialize Views
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        editAddress = findViewById(R.id.editAddress);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnInstructions).setOnClickListener(v -> showInstructionsDialog());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveDetails());

        loadExistingData();
    }

    private void showInstructionsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Quick Tips")
                .setMessage("• Use a professional email address.\n" +
                        "• Double-check your phone number.\n" +
                        "• Address is usually city and state/country.")
                .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadExistingData() {
        if (currentResumeId == -1) return;

        new Thread(() -> {
            currentResume = db.resumeDao().getResumeById(currentResumeId);
            if (currentResume != null) {
                runOnUiThread(() -> {
                    editName.setText(currentResume.getName());
                    editEmail.setText(currentResume.getEmail());
                    editPhone.setText(currentResume.getPhone());
                    editAddress.setText(currentResume.getAddress());
                });
            }
        }).start();
    }

    private void saveDetails() {
        if (currentResume == null) return;

        // Update object with user input
        currentResume.setName(editName.getText().toString().trim());
        currentResume.setEmail(editEmail.getText().toString().trim());
        currentResume.setPhone(editPhone.getText().toString().trim());
        currentResume.setAddress(editAddress.getText().toString().trim());

        new Thread(() -> {
            db.resumeDao().update(currentResume);
            runOnUiThread(() -> {
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                finish(); // Close activity and return to editor
            });
        }).start();
    }
}