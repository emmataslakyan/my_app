package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class TemplateSelectionActivity extends BaseActivity {

    private int currentResumeId = -1;
    private TemplateRepository repository;
    private TemplateAdapter adapter;
    private ProgressBar loading;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_selection);

        currentResumeId = getIntent().getIntExtra("RESUME_ID", -1);

        ImageButton backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        loading = findViewById(R.id.templatesLoading);
        empty = findViewById(R.id.templatesEmpty);
        RecyclerView recycler = findViewById(R.id.templatesRecycler);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new TemplateAdapter(this::onTemplatePicked);
        recycler.setAdapter(adapter);

        repository = new TemplateRepository(this);
        loadTemplates();
    }

    private void loadTemplates() {
        loading.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        repository.listTemplates(templates -> runOnUiThread(() -> {
            loading.setVisibility(View.GONE);
            adapter.submit(templates);
            empty.setVisibility(templates.isEmpty() ? View.VISIBLE : View.GONE);
        }));
    }

    private void onTemplatePicked(ResumeTemplate template) {
        String currentDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(TemplateSelectionActivity.this);
            Resume existingResume = db.resumeDao().getResumeById(currentResumeId);

            if (existingResume == null) {
                runOnUiThread(() -> Toast.makeText(this, "Error: Resume data lost", Toast.LENGTH_SHORT).show());
                return;
            }

            existingResume.setTemplateId(template.getId());
            existingResume.setDate(currentDate);
            db.resumeDao().update(existingResume);

            runOnUiThread(() -> {
                Toast.makeText(this, "Template Selected!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(TemplateSelectionActivity.this, ResumePreviewActivity.class);
                intent.putExtra("RESUME_ID", currentResumeId);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        });
    }
}
