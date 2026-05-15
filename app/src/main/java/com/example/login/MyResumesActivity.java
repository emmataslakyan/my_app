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
     *   - Room DB  → PersonalDetailsActivity reads Name/Email/Phone/Address
     *   - Firestore → ResumeEditorActivity's applySnapshotToPrefs fills SharedPreferences
     */
    private void prefillResumeInDb(int resumeId,
                                   Map<String, Object> profileData,
                                   UserProfileManager mgr) {
        new Thread(() -> {
            Resume r = db.resumeDao().getResumeById(resumeId);
            if (r != null) {
                String name    = getStr(profileData, UserProfileManager.KEY_FULL_NAME);
                String email   = getStr(profileData, UserProfileManager.KEY_EMAIL);
                String phone   = getStr(profileData, UserProfileManager.KEY_PHONE);
                String address = getStr(profileData, UserProfileManager.KEY_ADDRESS);

                if (!name.isEmpty())    r.setName(name);
                if (!email.isEmpty())   r.setEmail(email);
                if (!phone.isEmpty())   r.setPhone(phone);
                if (!address.isEmpty()) r.setAddress(address);

                db.resumeDao().update(r);
            }

            // Also write the Firestore snapshot (for SharedPreferences pre-fill)
            mgr.populateNewResume(
                    resumeId,
                    () -> runOnUiThread(() -> openEditor(resumeId)),
                    err -> runOnUiThread(() -> {
                        Toast.makeText(this,
                                "Profile could not be fully pre-filled.",
                                Toast.LENGTH_SHORT).show();
                        openEditor(resumeId);
                    }));
        }).start();
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