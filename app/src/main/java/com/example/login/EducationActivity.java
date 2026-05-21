package com.example.login;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EducationActivity extends BaseActivity {

    public static final String EXTRA_ENTRY_INDEX   = "ENTRY_INDEX";
    public static final String EXTRA_FIRESTORE_ID  = "ENTRY_FIRESTORE_ID";

    private TextInputEditText editSchool, editLocation, editDate, editDegree;
    private RecyclerView rvBullets;
    private BulletAdapter adapter;
    private final List<String> bulletList = new ArrayList<>();

    private ResumeRepository repo;
    private String currentResumeId;
    private int entryIndex = -1;
    private Resume currentResume;
    private List<EducationEntry> entries = new ArrayList<>();

    private UserProfileManager profileManager;
    private boolean isProfileMode;
    private String firestoreEntryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education);

        repo            = new ResumeRepository();
        profileManager  = new UserProfileManager();
        currentResumeId  = getIntent().getStringExtra("RESUME_ID");
        entryIndex       = getIntent().getIntExtra(EXTRA_ENTRY_INDEX, -1);
        firestoreEntryId = getIntent().getStringExtra(EXTRA_FIRESTORE_ID);
        isProfileMode    = ProfileActivity.PROFILE_RESUME_ID.equals(currentResumeId);

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
        if (firestoreEntryId == null || firestoreEntryId.isEmpty()) {
            if (bulletList.isEmpty()) bulletList.add("");
            adapter = new BulletAdapter(bulletList);
            rvBullets.setAdapter(adapter);
            return;
        }
        profileManager.loadEducation(list -> runOnUiThread(() -> {
            for (Map<String, Object> e : list) {
                if (firestoreEntryId.equals(e.get(UserProfileManager.EDU_ID))) {
                    editSchool.setText(getStr(e, UserProfileManager.EDU_SCHOOL));
                    editLocation.setText(getStr(e, UserProfileManager.EDU_LOCATION));
                    editDate.setText(getStr(e, UserProfileManager.EDU_DATE));
                    editDegree.setText(getStr(e, UserProfileManager.EDU_DEGREE));
                    String desc = getStr(e, UserProfileManager.EDU_DESCRIPTION);
                    if (!desc.isEmpty()) {
                        bulletList.addAll(Arrays.asList(desc.split("\\|")));
                    }
                    break;
                }
            }
            if (bulletList.isEmpty()) bulletList.add("");
            adapter = new BulletAdapter(bulletList);
            rvBullets.setAdapter(adapter);
        }), err -> runOnUiThread(() -> {
            if (bulletList.isEmpty()) bulletList.add("");
            adapter = new BulletAdapter(bulletList);
            rvBullets.setAdapter(adapter);
        }));
    }

    private void loadResumeEducation() {
        if (currentResumeId == null || currentResumeId.isEmpty()) return;
        repo.get(currentResumeId, r -> {
            currentResume = r;
            entries = ResumeEntries.parseEducation(r.getEducationJson());

            EducationEntry entry = (entryIndex >= 0 && entryIndex < entries.size())
                    ? entries.get(entryIndex) : new EducationEntry();

            if (entry.schoolDescription != null && !entry.schoolDescription.isEmpty()) {
                bulletList.addAll(Arrays.asList(entry.schoolDescription.split("\\|")));
            } else {
                bulletList.add("");
            }

            editSchool.setText(entry.schoolName);
            editLocation.setText(entry.schoolLocation);
            editDate.setText(entry.schoolDate);
            editDegree.setText(entry.degree);
            adapter = new BulletAdapter(bulletList);
            rvBullets.setAdapter(adapter);
        }, err -> Toast.makeText(this, "Couldn't load resume: " + err, Toast.LENGTH_SHORT).show());
    }

    private void saveEducation() {
        if (isProfileMode) {
            saveProfileEducation();
        } else {
            saveResumeEducation();
        }
    }

    private void saveProfileEducation() {
        profileManager.saveEducationEntry(
                firestoreEntryId,
                str(editSchool),
                str(editLocation),
                str(editDate),
                str(editDegree),
                buildActivitiesString(),
                () -> runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.education_saved), Toast.LENGTH_SHORT).show();
                    finish();
                }),
                err -> runOnUiThread(() ->
                        Toast.makeText(this, getString(R.string.error_save_failed, err), Toast.LENGTH_SHORT).show()));
    }

    private void saveResumeEducation() {
        if (currentResume == null) return;

        EducationEntry entry = (entryIndex >= 0 && entryIndex < entries.size())
                ? entries.get(entryIndex) : new EducationEntry();
        entry.schoolName = str(editSchool);
        entry.schoolLocation = str(editLocation);
        entry.schoolDate = str(editDate);
        entry.degree = str(editDegree);
        entry.schoolDescription = buildActivitiesString();

        if (entryIndex >= 0 && entryIndex < entries.size()) {
            entries.set(entryIndex, entry);
        } else {
            entries.add(entry);
        }
        currentResume.setEducationJson(ResumeEntries.serializeEducation(entries));

        repo.update(currentResume,
                () -> {
                    Toast.makeText(this, getString(R.string.education_saved), Toast.LENGTH_SHORT).show();
                    finish();
                },
                err -> Toast.makeText(this,
                        getString(R.string.error_save_failed, err),
                        Toast.LENGTH_SHORT).show());
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