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
import java.util.Map;

public class ExperienceActivity extends BaseActivity {

    public static final String EXTRA_ENTRY_INDEX   = "ENTRY_INDEX";
    public static final String EXTRA_FIRESTORE_ID  = "ENTRY_FIRESTORE_ID";

    private TextInputEditText editOrg, editPos, editLoc, editDate;
    private RecyclerView rvBullets;
    private BulletAdapter adapter;
    private final List<String> bulletList = new ArrayList<>();
    private AppDatabase db;
    private int resumeId;
    private int entryIndex = -1;
    private Resume resume;
    private List<ExperienceEntry> entries = new ArrayList<>();

    private UserProfileManager profileManager;
    private boolean isProfileMode;
    private String firestoreEntryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experience);

        // 1. Initialize Database and Data
        db = AppDatabase.getInstance(this);
        resumeId = getIntent().getIntExtra("RESUME_ID", -1);
        entryIndex = getIntent().getIntExtra(EXTRA_ENTRY_INDEX, -1);
        firestoreEntryId = getIntent().getStringExtra(EXTRA_FIRESTORE_ID);
        isProfileMode = (resumeId == ProfileActivity.PROFILE_RESUME_ID);
        profileManager = new UserProfileManager();

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

        findViewById(R.id.btnSave).setOnClickListener(v -> saveExperience());

        loadData();
    }

    private void loadProfileExperience() {
        if (firestoreEntryId == null || firestoreEntryId.isEmpty()) {
            if (bulletList.isEmpty()) bulletList.add("");
            adapter = new BulletAdapter(bulletList);
            rvBullets.setAdapter(adapter);
            return;
        }
        profileManager.loadExperience(list -> runOnUiThread(() -> {
            for (Map<String, Object> e : list) {
                if (firestoreEntryId.equals(e.get(UserProfileManager.EXP_ID))) {
                    editOrg.setText(getStr(e, UserProfileManager.EXP_ORG));
                    editPos.setText(getStr(e, UserProfileManager.EXP_POS));
                    editLoc.setText(getStr(e, UserProfileManager.EXP_LOC));
                    editDate.setText(getStr(e, UserProfileManager.EXP_DATE));
                    String bulls = getStr(e, UserProfileManager.EXP_BULLETS);
                    if (!bulls.isEmpty()) {
                        bulletList.addAll(Arrays.asList(bulls.split("\\|")));
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

    private void saveProfileExperience() {
        String org  = editOrg.getText()  != null ? editOrg.getText().toString()  : "";
        String pos  = editPos.getText()  != null ? editPos.getText().toString()  : "";
        String loc  = editLoc.getText()  != null ? editLoc.getText().toString()  : "";
        String date = editDate.getText() != null ? editDate.getText().toString() : "";
        StringBuilder sb = new StringBuilder();
        if (adapter != null) {
            for (String s : adapter.getBullets()) {
                if (s != null && !s.trim().isEmpty()) sb.append(s).append("|");
            }
        }
        profileManager.saveExperienceEntry(
                firestoreEntryId, org, pos, loc, date, sb.toString(),
                () -> runOnUiThread(() -> {
                    Toast.makeText(this, "Experience Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                }),
                err -> runOnUiThread(() ->
                        Toast.makeText(this, "Save failed: " + err, Toast.LENGTH_SHORT).show()));
    }

    private String getStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
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
        if (isProfileMode) { loadProfileExperience(); return; }
        if (resumeId == -1) return;

        new Thread(() -> {
            resume = db.resumeDao().getResumeById(resumeId);
            if (resume == null) return;
            entries = ResumeEntries.parseExperience(resume.getExperienceJson());

            ExperienceEntry entry = (entryIndex >= 0 && entryIndex < entries.size())
                    ? entries.get(entryIndex) : new ExperienceEntry();

            bulletList.clear();
            if (entry.expBullets != null && !entry.expBullets.isEmpty()) {
                bulletList.addAll(Arrays.asList(entry.expBullets.split("\\|")));
            } else {
                bulletList.add("");
            }

            runOnUiThread(() -> {
                editOrg.setText(entry.expOrgName);
                editPos.setText(entry.expPosition);
                editLoc.setText(entry.expLocation);
                editDate.setText(entry.expDate);

                adapter = new BulletAdapter(bulletList);
                rvBullets.setAdapter(adapter);
            });
        }).start();
    }

    private void saveExperience() {
        if (isProfileMode) { saveProfileExperience(); return; }
        if (resume == null) return;

        String org = editOrg.getText() != null ? editOrg.getText().toString() : "";
        String pos = editPos.getText() != null ? editPos.getText().toString() : "";
        String loc = editLoc.getText() != null ? editLoc.getText().toString() : "";
        String date = editDate.getText() != null ? editDate.getText().toString() : "";

        StringBuilder sb = new StringBuilder();
        if (adapter != null) {
            for (String s : adapter.getBullets()) {
                if (s != null && !s.trim().isEmpty()) {
                    sb.append(s).append("|");
                }
            }
        }

        ExperienceEntry entry = (entryIndex >= 0 && entryIndex < entries.size())
                ? entries.get(entryIndex) : new ExperienceEntry();
        entry.expOrgName = org;
        entry.expPosition = pos;
        entry.expLocation = loc;
        entry.expDate = date;
        entry.expBullets = sb.toString();

        if (entryIndex >= 0 && entryIndex < entries.size()) {
            entries.set(entryIndex, entry);
        } else {
            entries.add(entry);
        }
        resume.setExperienceJson(ResumeEntries.serializeExperience(entries));

        new Thread(() -> {
            db.resumeDao().update(resume);
            runOnUiThread(() -> {
                Toast.makeText(this, "Experience Saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}