package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MyResumesActivity extends BaseActivity {

    RecyclerView resumeRecyclerView;
    ResumeAdapter adapter;
    AppDatabase db;
    List<Resume> resumeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_resumes);

        ImageButton backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        db = AppDatabase.getInstance(this);
        resumeRecyclerView = findViewById(R.id.profileRecyclerView);
        resumeRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ResumeAdapter(resumeList, db);
        resumeRecyclerView.setAdapter(adapter);

        findViewById(R.id.createProfileBtn).setOnClickListener(v -> {
            // Use the Activity Name .class explicitly
            Intent intent = new Intent(MyResumesActivity.this, ResumeEditorActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadResumes();
    }

    public void loadResumes() {
        new Thread(() -> {
            List<Resume> latestList = db.resumeDao().getAllResumes();
            runOnUiThread(() -> {
                int oldSize = resumeList.size();
                resumeList.clear();
                resumeList.addAll(latestList);

                // If the list size is the same, just refresh everything
                // If not, this helps the RecyclerView animate the new items
                if (resumeList.size() != oldSize) {
                    adapter.notifyDataSetChanged();
                } else {
                    adapter.notifyItemRangeChanged(0, resumeList.size());
                }
            });
        }).start();
    }
}
// DO NOT ADD ANY OTHER CLASSES BELOW THIS LINE