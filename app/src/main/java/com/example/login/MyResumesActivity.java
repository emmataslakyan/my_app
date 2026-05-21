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
    private ResumeRepository repo;
    private final List<Resume> resumeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_resumes);

        repo = new ResumeRepository();

        ImageButton backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        resumeRecyclerView = findViewById(R.id.profileRecyclerView);
        resumeRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ResumeAdapter(resumeList, repo, this::loadResumes);
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
        Resume newResume = new Resume();
        newResume.setTitle("New Resume");
        newResume.setDate("Created: " +
                java.text.DateFormat.getDateInstance().format(new java.util.Date()));

        repo.create(newResume,
                newId -> {
                    UserProfileManager mgr = new UserProfileManager();
                    mgr.loadProfile(
                            profileData -> applyProfileToResume(newId, profileData, mgr),
                            err -> openEditor(newId));
                },
                err -> Toast.makeText(this, "Create failed: " + err, Toast.LENGTH_SHORT).show());
    }

    /** Populates a newly-created resume from the user's global profile, then opens the editor. */
    private void applyProfileToResume(String resumeId,
                                      Map<String, Object> profileData,
                                      UserProfileManager mgr) {
        repo.get(resumeId, r -> {
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
                String photoUrl = getStr(profileData, UserProfileManager.KEY_PHOTO_URL);
                if (!photoUrl.isEmpty()) r.setPhotoPath(photoUrl);
            }

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
                    r.setExperienceJson(ResumeEntries.serializeExperience(entries));
                }

                mgr.loadEducation(eduList -> {
                    if (!eduList.isEmpty()) {
                        List<EducationEntry> entries = new ArrayList<>();
                        for (Map<String, Object> e : eduList) {
                            EducationEntry entry = new EducationEntry();
                            entry.schoolName        = getStr(e, UserProfileManager.EDU_SCHOOL);
                            entry.schoolLocation    = getStr(e, UserProfileManager.EDU_LOCATION);
                            entry.schoolDate        = getStr(e, UserProfileManager.EDU_DATE);
                            entry.degree            = getStr(e, UserProfileManager.EDU_DEGREE);
                            entry.schoolDescription = getStr(e, UserProfileManager.EDU_DESCRIPTION);
                            entries.add(entry);
                        }
                        r.setEducationJson(ResumeEntries.serializeEducation(entries));
                    }
                    repo.update(r,
                            () -> openEditor(resumeId),
                            err -> openEditor(resumeId));
                }, err -> repo.update(r,
                        () -> openEditor(resumeId),
                        e2 -> openEditor(resumeId)));
            }, err -> repo.update(r,
                    () -> openEditor(resumeId),
                    e2 -> openEditor(resumeId)));

        }, err -> openEditor(resumeId));
    }

    private void openEditor(String resumeId) {
        Intent intent = new Intent(MyResumesActivity.this, ResumeEditorActivity.class);
        intent.putExtra("RESUME_ID", resumeId);
        startActivity(intent);
    }

    // ── Load resume list ──────────────────────────────────────────────────────

    public void loadResumes() {
        repo.getAll(latestList -> {
            resumeList.clear();
            resumeList.addAll(latestList);
            adapter.notifyDataSetChanged();
        }, err -> android.util.Log.e("MyResumesActivity", "Load failed: " + err));
    }

    private String getStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}
