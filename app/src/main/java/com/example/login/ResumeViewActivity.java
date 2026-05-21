package com.example.login;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.Map;

public class ResumeViewActivity extends BaseActivity {

    private String resumeId;
    private ResumeRepository resumeRepo;
    private WebView webView;
    private TemplateRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume_view);

        resumeId = getIntent().getStringExtra("RESUME_ID");
        resumeRepo = new ResumeRepository();
        repository = new TemplateRepository(this);

        View backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        webView = findViewById(R.id.resumeWebView);
        if (webView != null) {
            WebSettings s = webView.getSettings();
            s.setJavaScriptEnabled(false);
            s.setAllowFileAccess(true);
            s.setLoadWithOverviewMode(true);
            s.setUseWideViewPort(true);
        }

        loadResumeData();
    }

    private void loadResumeData() {
        if (resumeId == null || resumeId.isEmpty()) {
            Toast.makeText(this, "Resume not found", Toast.LENGTH_SHORT).show();
            return;
        }
        resumeRepo.get(resumeId, resume -> {
            if (webView == null) return;
            ResumeTemplate tpl = resolveTemplate(resume.getTemplateId());
            Map<String, Object> ctx = ResumeDataMapper.toContext(resume);
            repository.loadTemplateHtml(tpl, new TemplateRepository.HtmlCallback() {
                @Override public void onHtml(String html, String baseUrl) {
                    String rendered = MustacheRenderer.render(html, ctx);
                    runOnUiThread(() -> webView.loadDataWithBaseURL(
                            baseUrl, rendered, "text/html", "UTF-8", null));
                }
                @Override public void onError(Exception e) {
                    runOnUiThread(() -> Toast.makeText(ResumeViewActivity.this,
                            "Couldn't load template", Toast.LENGTH_SHORT).show());
                }
            });
        }, err -> Toast.makeText(this, "Resume not found", Toast.LENGTH_SHORT).show());
    }

    private ResumeTemplate resolveTemplate(String id) {
        if (id == null || id.isEmpty() || ResumeTemplate.DEFAULT_ID.equals(id)) {
            return repository.bundledDefault();
        }
        return new ResumeTemplate(id, id, 1, null, "resume_templates/" + id, false);
    }
}
