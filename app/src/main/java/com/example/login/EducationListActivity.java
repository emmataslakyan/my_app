package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EducationListActivity extends BaseActivity {

    private ResumeRepository repo;
    private String resumeId;
    private Resume resume;
    private List<EducationEntry> entries = new ArrayList<>();
    private final List<String> firestoreIds = new ArrayList<>();
    private EducationListAdapter adapter;

    private RecyclerView rv;
    private TextView emptyView;

    private UserProfileManager profileManager;
    private boolean isProfileMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education_list);

        repo = new ResumeRepository();
        resumeId = getIntent().getStringExtra("RESUME_ID");
        isProfileMode = ProfileActivity.PROFILE_RESUME_ID.equals(resumeId);
        profileManager = new UserProfileManager();

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddEntry).setOnClickListener(v -> openEditor(-1, null));

        rv = findViewById(R.id.rvEntries);
        emptyView = findViewById(R.id.emptyView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EducationListAdapter(entries, new EducationListAdapter.Listener() {
            @Override
            public void onEntryClick(int position) {
                String fsId = (isProfileMode && position < firestoreIds.size())
                        ? firestoreIds.get(position) : null;
                openEditor(position, fsId);
            }
            @Override
            public void onEntryDelete(int position) {
                confirmDelete(position);
            }
        });
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        if (isProfileMode) { reloadFromFirestore(); return; }
        if (resumeId == null || resumeId.isEmpty()) return;
        repo.get(resumeId, r -> {
            resume = r;
            entries.clear();
            entries.addAll(ResumeEntries.parseEducation(r.getEducationJson()));
            adapter.notifyDataSetChanged();
            emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        }, err -> {
            entries.clear();
            adapter.notifyDataSetChanged();
            emptyView.setVisibility(View.VISIBLE);
        });
    }

    private void reloadFromFirestore() {
        profileManager.loadEducation(list -> runOnUiThread(() -> {
            entries.clear();
            firestoreIds.clear();
            for (Map<String, Object> e : list) {
                EducationEntry entry = new EducationEntry();
                entry.schoolName        = getStr(e, UserProfileManager.EDU_SCHOOL);
                entry.schoolLocation    = getStr(e, UserProfileManager.EDU_LOCATION);
                entry.schoolDate        = getStr(e, UserProfileManager.EDU_DATE);
                entry.degree            = getStr(e, UserProfileManager.EDU_DEGREE);
                entry.schoolDescription = getStr(e, UserProfileManager.EDU_DESCRIPTION);
                entries.add(entry);
                firestoreIds.add(getStr(e, UserProfileManager.EDU_ID));
            }
            adapter.notifyDataSetChanged();
            emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        }), err -> {});
    }

    private void openEditor(int index, String firestoreId) {
        Intent i = new Intent(this, EducationActivity.class);
        i.putExtra("RESUME_ID", resumeId);
        i.putExtra(EducationActivity.EXTRA_ENTRY_INDEX, index);
        if (firestoreId != null) i.putExtra(EducationActivity.EXTRA_FIRESTORE_ID, firestoreId);
        startActivity(i);
    }

    private void confirmDelete(int position) {
        if (position < 0 || position >= entries.size()) return;
        String name = entries.get(position).schoolName;
        new AlertDialog.Builder(this)
                .setTitle("Delete entry")
                .setMessage("Delete " + (name == null || name.trim().isEmpty() ? "this entry" : name) + "?")
                .setPositiveButton("Delete", (d, w) -> performDelete(position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(int position) {
        if (position < 0 || position >= entries.size()) return;
        if (isProfileMode) {
            String fsId = position < firestoreIds.size() ? firestoreIds.get(position) : null;
            if (fsId != null) {
                profileManager.deleteEducationEntry(fsId, () -> {}, err -> {});
            }
            entries.remove(position);
            firestoreIds.remove(position);
            adapter.notifyItemRemoved(position);
            emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }
        if (resume == null) return;
        entries.remove(position);
        adapter.notifyItemRemoved(position);
        emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);

        resume.setEducationJson(ResumeEntries.serializeEducation(entries));
        repo.update(resume,
                () -> {},
                err -> Toast.makeText(this, "Delete failed: " + err, Toast.LENGTH_SHORT).show());
    }

    private String getStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}
