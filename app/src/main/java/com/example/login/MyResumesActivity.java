package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MyResumesActivity extends BaseActivity {

    private RecyclerView resumeRecyclerView;
    private ResumeAdapter adapter;
    private AppDatabase db;
    private final List<Resume> resumeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_resumes);

        // 1. Initialize Database
        db = AppDatabase.getInstance(this);

        // 2. Setup UI
        ImageButton backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        resumeRecyclerView = findViewById(R.id.profileRecyclerView);
        resumeRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3. Setup Adapter
        adapter = new ResumeAdapter(resumeList, db);
        resumeRecyclerView.setAdapter(adapter);

        // 4. Create Button Logic (The "Resume Builder" part)
        findViewById(R.id.createProfileBtn).setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    // 1. Create the entry
                    Resume newResume = new Resume();
                    newResume.setTitle("New Resume");
                    newResume.setDate("Created: " + java.text.DateFormat.getDateInstance().format(new java.util.Date()));

                    // 2. Insert and get ID
                    long newId = db.resumeDao().insert(newResume);

                    // 3. Move to Editor
                    runOnUiThread(() -> {
                        Intent intent = new Intent(MyResumesActivity.this, ResumeEditorActivity.class);
                        intent.putExtra("RESUME_ID", (int) newId);
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    android.util.Log.e("DB_ERROR", "Insert failed: " + e.getMessage());
                }
            }).start();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadResumes(); // Refresh list whenever we come back to this screen
    }

    // Change 'private' to 'public' so the Adapter can see it
    // Ensure it says 'public' at the start
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
}