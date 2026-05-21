package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ResumePreviewActivity extends BaseActivity {

    private String currentResumeId;
    private WebView webView;
    private ProgressBar loading;
    private boolean templateLoaded;

    private TemplateRepository repository;
    private ResumeRepository resumeRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume_preview);

        currentResumeId = getIntent().getStringExtra("RESUME_ID");
        repository = new TemplateRepository(this);
        resumeRepo = new ResumeRepository();

        webView = findViewById(R.id.resumeWebView);
        loading = findViewById(R.id.resumeLoading);
        configureWebView();

        setupButton(R.id.backBtn,     v -> finish());
        setupButton(R.id.downloadBtn, v -> printResume());
        setupButton(R.id.btnEdit,     v -> finish());
        setupButton(R.id.btnShare,    v -> printResume());
        setupButton(R.id.btnZoom,     v -> showZoomDialog());

        loadResume();
    }

    private void setupButton(int id, View.OnClickListener listener) {
        View btn = findViewById(id);
        if (btn != null) btn.setOnClickListener(listener);
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                loading.setVisibility(View.GONE);
                templateLoaded = true;
            }
        });
    }

    private void loadResume() {
        loading.setVisibility(View.VISIBLE);
        templateLoaded = false;
        if (currentResumeId == null || currentResumeId.isEmpty()) {
            loading.setVisibility(View.GONE);
            Toast.makeText(this, "Resume not found", Toast.LENGTH_SHORT).show();
            return;
        }
        resumeRepo.get(currentResumeId, resume -> {
            // Fall back to the user's profile photo (Firestore) if this resume has none.
            if (resume.getPhotoPath() == null || resume.getPhotoPath().isEmpty()) {
                new Thread(() -> {
                    String url = fetchProfilePhotoUrlBlocking();
                    if (url != null && !url.isEmpty()) resume.setPhotoPath(url);
                    runOnUiThread(() -> renderResume(resume));
                }).start();
            } else {
                renderResume(resume);
            }
        }, err -> {
            loading.setVisibility(View.GONE);
            Toast.makeText(this, "Resume not found", Toast.LENGTH_SHORT).show();
        });
    }

    private void renderResume(Resume resume) {
        ResumeTemplate tpl = resolveTemplate(resume.getTemplateId());
        Map<String, Object> ctx = ResumeDataMapper.toContext(resume);
        repository.loadTemplateHtml(tpl, new TemplateRepository.HtmlCallback() {
            @Override public void onHtml(String html, String baseUrl) {
                String rendered = MustacheRenderer.render(html, ctx);
                runOnUiThread(() -> webView.loadDataWithBaseURL(
                        baseUrl, rendered, "text/html", "UTF-8", null));
            }
            @Override public void onError(Exception e) {
                runOnUiThread(() -> {
                    loading.setVisibility(View.GONE);
                    Toast.makeText(ResumePreviewActivity.this,
                            "Couldn't load template", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Blocking lookup of the user's profile photo URL from Firestore. Called from a
     * worker thread during resume preview render so the resume's empty photoPath can
     * be seeded with the latest cross-device value.
     */
    private String fetchProfilePhotoUrlBlocking() {
        final String[] result = {null};
        final CountDownLatch latch = new CountDownLatch(1);
        new UserProfileManager().loadProfile(
                data -> {
                    Object v = data.get(UserProfileManager.KEY_PHOTO_URL);
                    if (v != null) result[0] = v.toString();
                    latch.countDown();
                },
                err -> latch.countDown());
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        return result[0];
    }

    private ResumeTemplate resolveTemplate(String id) {
        if (id == null || id.isEmpty() || ResumeTemplate.DEFAULT_ID.equals(id)) {
            return repository.bundledDefault();
        }
        // Minimal: rely on TemplateRepository to fall back to bundled if the remote one fails.
        return new ResumeTemplate(id, id, 1, null, "resume_templates/" + id, false);
    }

    // ── PDF / print ───────────────────────────────────────────────────────────

    private void printResume() {
        if (!templateLoaded) {
            Toast.makeText(this, "Preview is still loading…", Toast.LENGTH_SHORT).show();
            return;
        }
        PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
        if (pm == null) {
            Toast.makeText(this, "Print service unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        String jobName = "SkillSpark_Resume";
        PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(jobName);
        PrintAttributes attrs = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
        pm.print(jobName, adapter, attrs);
    }

    // ── Zoom ──────────────────────────────────────────────────────────────────

    private void showZoomDialog() {
        String[] options = {"75%", "100% (Default)", "125%", "150%"};
        new AlertDialog.Builder(this)
                .setTitle("Zoom")
                .setItems(options, (dialog, which) -> {
                    int[] scales = {75, 100, 125, 150};
                    webView.setInitialScale(scales[which]);
                    webView.reload();
                })
                .show();
    }
}
