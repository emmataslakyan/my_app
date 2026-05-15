package com.example.login;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EducationActivity extends BaseActivity {

    private TextInputEditText editSchool, editLocation, editDate, editDegree;
    private RecyclerView rvBullets;
    private BulletAdapter adapter;
    private final List<String> bulletList = new ArrayList<>();

    private AppDatabase db;
    private int currentResumeId;
    private Resume currentResume;

    private UserProfileManager profileManager;
    private boolean isProfileMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education);

        db              = AppDatabase.getInstance(this);
        profileManager  = new UserProfileManager();
        currentResumeId = getIntent().getIntExtra("RESUME_ID", -1);
        isProfileMode   = (currentResumeId == ProfileActivity.PROFILE_RESUME_ID);

        editSchool   = findViewById(R.id.editSchool);
        editLocation = findViewById(R.id.editSchoolLocation);
        editDate     = findViewById(R.id.editSchoolDate);
        editDegree   = findViewById(R.id.editDegree);

        rvBullets = findViewById(R.id.rvEducationBullets);
        rvBullets.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BulletAdapter(bulletList);
        rvBullets.setAdapter(adapter);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnUpgradeHint).setOnClickListener(v -> showEducationGuide());
        findViewById(R.id.btnAddEduBullet).setOnClickListener(v -> adapter.addBullet());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveEducation());

        loadData();
    }

    private void loadData() {
        if (isProfileMode) {
            loadProfileEducation();
        } else {
            loadResumeEducation();
        }
    }

    private void loadProfileEducation() {
        profileManager.loadEducation(
                list -> runOnUiThread(() -> {
                    if (!list.isEmpty()) {
                        Map<String, Object> e = list.get(0);
                        editSchool.setText(getStr(e, UserProfileManager.EDU_SCHOOL));
                        editDegree.setText(getStr(e, UserProfileManager.EDU_DEGREE));
                        editLocation.setText(getStr(e, "location"));
                        editDate.setText(getStr(e, UserProfileManager.EDU_START));
                        String acts = getStr(e, "activities");
                        if (!acts.isEmpty()) {
                            bulletList.addAll(Arrays.asList(acts.split("\\|")));
                        }
                    }
                    if (bulletList.isEmpty()) bulletList.add("");
                    adapter = new BulletAdapter(bulletList);
                    rvBullets.setAdapter(adapter);
                }),
                err -> runOnUiThread(() -> {
                    if (bulletList.isEmpty()) bulletList.add("");
                    adapter = new BulletAdapter(bulletList);
                    rvBullets.setAdapter(adapter);
                })
        );
    }

    private void loadResumeEducation() {
        if (currentResumeId == -1) return;
        new Thread(() -> {
            currentResume = db.resumeDao().getResumeById(currentResumeId);
            if (currentResume != null) {
                String saved = currentResume.getSchoolDescription();
                if (saved != null && !saved.isEmpty()) {
                    bulletList.addAll(Arrays.asList(saved.split("\\|")));
                } else {
                    bulletList.add("");
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
        if (isProfileMode) {
            saveProfileEducation();
        } else {
            saveResumeEducation();
        }
    }

    private void saveProfileEducation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.error_not_signed_in), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> entry = new HashMap<>();
        entry.put(UserProfileManager.EDU_SCHOOL,  str(editSchool));
        entry.put(UserProfileManager.EDU_DEGREE,  str(editDegree));
        entry.put("location",                     str(editLocation));
        entry.put(UserProfileManager.EDU_START,   str(editDate));
        entry.put(UserProfileManager.EDU_END,     "");
        entry.put("activities",                   buildActivitiesString());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("education")
                .document("entry_0")
                .set(entry)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, getString(R.string.education_saved), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_save_failed, e.getMessage()), Toast.LENGTH_SHORT).show()
                );
    }

    private void saveResumeEducation() {
        if (currentResume == null) return;
        currentResume.setSchoolName(str(editSchool));
        currentResume.setSchoolLocation(str(editLocation));
        currentResume.setSchoolDate(str(editDate));
        currentResume.setDegree(str(editDegree));
        currentResume.setSchoolDescription(buildActivitiesString());

        new Thread(() -> {
            db.resumeDao().update(currentResume);
            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.education_saved), Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    private String str(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private String getStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private String buildActivitiesString() {
        StringBuilder sb = new StringBuilder();
        if (adapter != null) {
            for (String s : adapter.getBullets()) {
                if (s != null && !s.trim().isEmpty()) sb.append(s).append("|");
            }
        }
        return sb.toString();
    }

    private void showEducationGuide() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        ViewGroup parent = findViewById(android.R.id.content);
        android.view.View v = getLayoutInflater().inflate(R.layout.hint_bottom_sheet, parent, false);

        android.widget.TextView hint = v.findViewById(R.id.hintContent);
        hint.setText(getString(R.string.education_hints));

        sheet.setContentView(v);
        sheet.show();
    }
}