package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ExperienceListActivity extends BaseActivity {

    private AppDatabase db;
    private int resumeId;
    private Resume resume;
    private List<ExperienceEntry> entries = new ArrayList<>();
    private ExperienceListAdapter adapter;

    private RecyclerView rv;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experience_list);

        db = AppDatabase.getInstance(this);
        resumeId = getIntent().getIntExtra("RESUME_ID", -1);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddEntry).setOnClickListener(v -> openEditor(-1));

        rv = findViewById(R.id.rvEntries);
        emptyView = findViewById(R.id.emptyView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ExperienceListAdapter(entries, new ExperienceListAdapter.Listener() {
            @Override
            public void onEntryClick(int position) {
                openEditor(position);
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
        if (resumeId == -1) return;
        new Thread(() -> {
            resume = db.resumeDao().getResumeById(resumeId);
            List<ExperienceEntry> parsed = (resume == null)
                    ? new ArrayList<>()
                    : ResumeEntries.parseExperience(resume.getExperienceJson());
            runOnUiThread(() -> {
                entries.clear();
                entries.addAll(parsed);
                adapter.notifyDataSetChanged();
                emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void openEditor(int index) {
        Intent i = new Intent(this, ExperienceActivity.class);
        i.putExtra("RESUME_ID", resumeId);
        i.putExtra(ExperienceActivity.EXTRA_ENTRY_INDEX, index);
        startActivity(i);
    }

    private void confirmDelete(int position) {
        if (position < 0 || position >= entries.size()) return;
        String name = entries.get(position).expOrgName;
        new AlertDialog.Builder(this)
                .setTitle("Delete entry")
                .setMessage("Delete " + (name == null || name.trim().isEmpty() ? "this entry" : name) + "?")
                .setPositiveButton("Delete", (d, w) -> performDelete(position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(int position) {
        if (resume == null || position < 0 || position >= entries.size()) return;
        entries.remove(position);
        adapter.notifyItemRemoved(position);
        emptyView.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);

        resume.setExperienceJson(ResumeEntries.serializeExperience(entries));
        new Thread(() -> db.resumeDao().update(resume)).start();
    }
}
