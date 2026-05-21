package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyResumesActivity extends BaseActivity {

    private RecyclerView resumeRecyclerView;
    private ResumeAdapter adapter;
    private AppDatabase db;
    private final List<Resume> resumeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_resumes);

        db = AppDatabase.getInstance(this);

        ImageButton backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        resumeRecyclerView = findViewById(R.id.profileRecyclerView);
        resumeRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ResumeAdapter(resumeList, db);
        resumeRecyclerView.setAdapter(adapter);

        findViewById(R.id.createProfileBtn).setOnClickListener(v -> createNewResume());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadResumes();
    }

    // ── Create resume with auto-populated profile data ────────────────────────

    private void createNewResume() {
        new Thread(() -> {
            try {
                // 1. Insert blank resume into Room DB
                Resume newResume = new Resume();
                newResume.setTitle("New Resume");
                newResume.setDate("Created: " +
                        java.text.DateFormat.getDateInstance().format(new java.util.Date()));
                long newId = db.resumeDao().insert(newResume);

                // 2. Load the user's global profile from Firestore and pre-fill Room DB.
                //    This is what makes PersonalDetailsActivity open with data already there.
                UserProfileManager mgr = new UserProfileManager();
                mgr.loadProfile(
                        profileData -> prefillResumeInDb((int) newId, profileData, mgr),
                        err -> {
                            // No profile yet — open editor anyway
                            runOnUiThread(() -> openEditor((int) newId));
                        });

            } catch (Exception e) {
                android.util.Log.e("MyResumesActivity", "Insert failed: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Writes the global profile data into the Room DB Resume row AND into the
     * Firestore resume snapshot. Both are needed:
     *   - Room DB  → all section activities read their data from here
     *   - Firestore → ResumeEditorActivity's applySnapshotToPrefs fills SharedPreferences
     */
    private void prefillResumeInDb(int resumeId,
                                   Map<String, Object> profileData,
                                   UserProfileManager mgr) {
        // Step 1: apply profile fields to Room DB row, then chain experience and education loads
        applyProfileToResume(resumeId, profileData, mgr);
    }

    private void applyProfileToResume(int resumeId,
                                      Map<String, Object> profileData,
                                      UserProfileManager mgr) {
        new Thread(() -> {
            Resume r = db.resumeDao().getResumeById(resumeId);
            if (r == null) { runOnUiThread(() -> openEditor(resumeId)); return; }

            String name      = getStr(profileData, UserProfileManager.KEY_FULL_NAME);
            String email     = getStr(profileData, UserProfileManager.KEY_EMAIL);
            String phone     = getStr(profileData, UserProfileManager.KEY_PHONE);
            String address   = getStr(profileData, UserProfileManager.KEY_ADDRESS);
            String skills    = getStr(profileData, UserProfileManager.KEY_SKILLS);
            String languages = getStr(profileData, UserProfileManager.KEY_LANGUAGES);

            if (!name.isEmpty())      r.setName(name);
            if (!email.isEmpty())     r.setEmail(email);
            if (!phone.isEmpty())     r.setPhone(phone);
            if (!address.isEmpty())   r.setAddress(address);
            if (!skills.isEmpty())    r.setSkills(skills);
            if (!languages.isEmpty()) r.setLanguages(languages);

            if (r.getPhotoPath() == null || r.getPhotoPath().isEmpty()) {
                String globalPhoto = getSharedPreferences("ProfilePrefs", MODE_PRIVATE)
                        .getString("profile_photo_path", null);
                if (globalPhoto != null && new java.io.File(globalPhoto).exists()) {
                    r.setPhotoPath(globalPhoto);
                }
            }

            db.resumeDao().update(r);
        }).start();

        // Step 2: load experience from Firestore and write to Room DB
        mgr.loadExperience(expList -> {
            if (!expList.isEmpty()) {
                List<ExperienceEntry> entries = new ArrayList<>();
                for (Map<String, Object> e : expList) {
                    ExperienceEntry entry = new ExperienceEntry();
                    entry.expOrgName  = getStr(e, UserProfileManager.EXP_ORG);
                    entry.expPosition = getStr(e, UserProfileManager.EXP_POS);
                    entry.expLocation = getStr(e, UserProfileManager.EXP_LOC);
                    entry.expDate     = getStr(e, UserProfileManager.EXP_DATE);
                    entry.expBullets  = getStr(e, UserProfileManager.EXP_BULLETS);
                    entries.add(entry);
                }
                String expJson = ResumeEntries.serializeExperience(entries);
                new Thread(() -> {
                    Resume r = db.resumeDao().getResumeById(resumeId);
                    if (r != null) { r.setExperienceJson(expJson); db.resumeDao().update(r); }
                }).start();
            }
        }, err -> {});

        // Step 3: load education from Firestore and write to Room DB
        mgr.loadEducation(eduList -> {
            if (!eduList.isEmpty()) {
                List<EducationEntry> entries = new ArrayList<>();
                for (Map<String, Object> e : eduList) {
                    EducationEntry entry = new EducationEntry();
                    entry.schoolName        = getStr(e, UserProfileManager.EDU_SCHOOL);
                    entry.degree            = getStr(e, UserProfileManager.EDU_DEGREE);
                    entry.schoolLocation    = getStr(e, "location");
                    entry.schoolDate        = getStr(e, UserProfileManager.EDU_START);
                    entry.schoolDescription = getStr(e, "activities");
                    entries.add(entry);
                }
                String eduJson = ResumeEntries.serializeEducation(entries);
                new Thread(() -> {
                    Resume r = db.resumeDao().getResumeById(resumeId);
                    if (r != null) { r.setEducationJson(eduJson); db.resumeDao().update(r); }
                }).start();
            }
        }, err -> {});

        // Step 4: write the Firestore snapshot (for SharedPreferences pre-fill) and open editor
        mgr.populateNewResume(
                resumeId,
                () -> runOnUiThread(() -> openEditor(resumeId)),
                err -> runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Profile could not be fully pre-filled.",
                            Toast.LENGTH_SHORT).show();
                    openEditor(resumeId);
                }));
    }

    private void openEditor(int resumeId) {
        Intent intent = new Intent(MyResumesActivity.this, ResumeEditorActivity.class);
        intent.putExtra("RESUME_ID", resumeId);
        startActivity(intent);
    }

    // ── Load resume list ──────────────────────────────────────────────────────

    public void loadResumes() {
        new Thread(() -> {
            List<Resume> latestList = db.resumeDao().getAllResumes();
            runOnUiThread(() -> {
                resumeList.clear();
                resumeList.addAll(latestList);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String getStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}