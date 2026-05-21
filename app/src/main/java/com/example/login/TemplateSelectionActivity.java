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

public class TemplateSelectionActivity extends BaseActivity {

    private String currentResumeId;
    private TemplateRepository repository;
    private ResumeRepository resumeRepo;
    private TemplateAdapter adapter;
    private ProgressBar loading;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_selection);

        currentResumeId = getIntent().getStringExtra("RESUME_ID");

        ImageButton backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        loading = findViewById(R.id.templatesLoading);
        empty = findViewById(R.id.templatesEmpty);
        RecyclerView recycler = findViewById(R.id.templatesRecycler);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new TemplateAdapter(this::onTemplatePicked);
        recycler.setAdapter(adapter);

        repository = new TemplateRepository(this);
        resumeRepo = new ResumeRepository();
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
        if (currentResumeId == null || currentResumeId.isEmpty()) {
            Toast.makeText(this, "Error: Resume id missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());

        resumeRepo.get(currentResumeId, existing -> {
            existing.setTemplateId(template.getId());
            existing.setDate(currentDate);
            resumeRepo.update(existing, () -> {
                Toast.makeText(this, "Template Selected!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(TemplateSelectionActivity.this, ResumePreviewActivity.class);
                intent.putExtra("RESUME_ID", currentResumeId);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }, err -> Toast.makeText(this, "Save failed: " + err, Toast.LENGTH_SHORT).show());
        }, err -> Toast.makeText(this, "Error: Resume data lost", Toast.LENGTH_SHORT).show());
    }
}
