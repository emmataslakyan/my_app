package com.example.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ResumePreviewActivity extends BaseActivity {

    private static final String TAG = "ResumePreviewActivity";
    private int currentResumeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume_preview);

        currentResumeId = getIntent().getIntExtra("RESUME_ID", -1);

        setupButton(R.id.backBtn,     v -> finish());
        setupButton(R.id.downloadBtn, v -> generatePDF());
        setupButton(R.id.btnEdit,     v -> finish());
        setupButton(R.id.btnShare,    v -> sharePDF());
        setupButton(R.id.btnTextSize, v -> showTextSizeSheet());
        setupButton(R.id.btnZoom,     v -> showZoomDialog());

        loadResumeData();
    }

    private void setupButton(int id, View.OnClickListener listener) {
        View btn = findViewById(id);
        if (btn != null) btn.setOnClickListener(listener);
    }

    // ── Section label helper ──────────────────────────────────────────────────

    /**
     * Reads user-renamed section titles from the same SharedPreferences that
     * ResumeEditorActivity writes to, falling back to the default if not set.
     */
    private String getSectionName(String prefKey, String defaultValue) {
        SharedPreferences prefs = getSharedPreferences(
                "CustomNames_" + currentResumeId, MODE_PRIVATE);
        String value = prefs.getString(prefKey, defaultValue);
        return (value != null && !value.trim().isEmpty())
                ? value.trim().toUpperCase()
                : defaultValue.toUpperCase();
    }

    // ── Text Size Dialog ──────────────────────────────────────────────────────

    private void showTextSizeSheet() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_text_size, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(v)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Spinner spinnerFontSize    = v.findViewById(R.id.spinnerFontSize);
        Spinner spinnerNameSize    = v.findViewById(R.id.spinnerNameSize);
        Spinner spinnerHeadingSize = v.findViewById(R.id.spinnerHeadingSize);
        Spinner spinnerFontStyle   = v.findViewById(R.id.spinnerFontStyle);
        Spinner spinnerLineSpacing = v.findViewById(R.id.spinnerLineSpacing);
        Spinner spinnerAlignment   = v.findViewById(R.id.spinnerAlignment);

        setupSpinner(spinnerFontSize,    new String[]{"8pt","9pt","10pt","11pt","12pt","13pt"}, 3);
        setupSpinner(spinnerNameSize,    new String[]{"14pt","15pt","16pt","17pt","18pt","20pt"}, 3);
        setupSpinner(spinnerHeadingSize, new String[]{"9pt","10pt","11pt","12pt","13pt"}, 1);
        setupSpinner(spinnerFontStyle,   new String[]{"Arial","Serif","Monospace","Sans-Serif"}, 0);
        setupSpinner(spinnerLineSpacing, new String[]{"1","1.15","1.5","2"}, 0);
        setupSpinner(spinnerAlignment,   new String[]{"Justify","Left","Center","Right"}, 0);

        v.findViewById(R.id.btnApply).setOnClickListener(btn -> {
            float[] bodyVals    = {8f, 9f, 10f, 11f, 12f, 13f};
            float[] nameVals    = {14f, 15f, 16f, 17f, 18f, 20f};
            float[] headingVals = {9f, 10f, 11f, 12f, 13f};
            float[] spacingVals = {0f, 2f, 6f, 12f};
            int[]   alignVals   = {
                    android.view.Gravity.START | android.view.Gravity.END,
                    android.view.Gravity.START,
                    android.view.Gravity.CENTER_HORIZONTAL,
                    android.view.Gravity.END
            };

            applyTypography(
                    bodyVals[spinnerFontSize.getSelectedItemPosition()],
                    nameVals[spinnerNameSize.getSelectedItemPosition()],
                    headingVals[spinnerHeadingSize.getSelectedItemPosition()],
                    spinnerFontStyle.getSelectedItemPosition(),
                    spacingVals[spinnerLineSpacing.getSelectedItemPosition()],
                    alignVals[spinnerAlignment.getSelectedItemPosition()]
            );
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupSpinner(Spinner spinner, String[] items, int defaultPos) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(defaultPos);
    }

    private void applyTypography(float body, float name, float heading,
                                 int stylePos, float extraSpacing, int gravity) {
        Typeface base = switch (stylePos) {
            case 1  -> Typeface.SERIF;
            case 2  -> Typeface.MONOSPACE;
            default -> Typeface.SANS_SERIF;
        };

        applyTv(R.id.previewName,        name,    base, extraSpacing, gravity);
        applyTv(R.id.previewContactInfo, body,    base, extraSpacing, gravity);
        applyTv(R.id.previewEduName,     heading, base, extraSpacing, gravity);
        applyTv(R.id.previewEduLocation, heading, base, extraSpacing, gravity);
        applyTv(R.id.previewEduDesc,     body,    base, extraSpacing, gravity);
        applyTv(R.id.previewExpName,     heading, base, extraSpacing, gravity);
        applyTv(R.id.previewExpDesc,     body,    base, extraSpacing, gravity);
        applyTv(R.id.previewSkills,      body,    base, extraSpacing, gravity);
    }

    private void applyTv(int viewId, float sp, Typeface base,
                         float extraSpacingPx, int gravity) {
        TextView tv = findViewById(viewId);
        if (tv == null) return;
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        int existingStyle = tv.getTypeface() != null
                ? tv.getTypeface().getStyle() : Typeface.NORMAL;
        tv.setTypeface(Typeface.create(base, existingStyle));
        tv.setLineSpacing(extraSpacingPx, 1f);
        if (viewId == R.id.previewEduDesc
                || viewId == R.id.previewExpDesc
                || viewId == R.id.previewSkills) {
            tv.setGravity(gravity | android.view.Gravity.TOP);
        }
    }

    // ── Zoom ──────────────────────────────────────────────────────────────────

    private void showZoomDialog() {
        String[] options = {"75%", "100% (Default)", "125%", "150%"};
        new AlertDialog.Builder(this)
                .setTitle("Zoom")
                .setItems(options, (dialog, which) -> {
                    float[] scales = {0.75f, 1.0f, 1.25f, 1.5f};
                    View paper = findViewById(R.id.resumePaper);
                    if (paper != null) {
                        paper.setScaleX(scales[which]);
                        paper.setScaleY(scales[which]);
                    }
                })
                .show();
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadResumeData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Resume resume = db.resumeDao().getResumeById(currentResumeId);
            if (resume != null) {
                runOnUiThread(() -> populateUI(resume));
            }
        }).start();
    }

    private void populateUI(Resume resume) {

        // ── Section Labels — read from same prefs as ResumeEditorActivity ────
        updateText(R.id.labelEducation,   getSectionName("name_edu",      "Education"));
        updateText(R.id.labelExperience,  getSectionName("name_exp",      "Experience"));
        updateText(R.id.labelSkills,      getSectionName("name_skills",   "Skills"));
        updateText(R.id.labelVolunteering,getSectionName("name_vol",      "Volunteering"));
        updateText(R.id.labelProjects,    getSectionName("name_projects", "Projects"));
        updateText(R.id.labelLanguages,   getSectionName("name_lang",     "Languages"));
        updateText(R.id.labelAwards,      getSectionName("name_awards",   "Honors & Awards"));

        // ── Header ────────────────────────────────────────────────────────────
        updateText(R.id.previewName, resume.getName());

        StringBuilder contact = new StringBuilder();
        appendIfPresent(contact, resume.getAddress());
        appendIfPresent(contact, resume.getEmail());
        appendIfPresent(contact, resume.getPhone());
        updateText(R.id.previewContactInfo, contact.toString());

        // ── Education ─────────────────────────────────────────────────────────
        updateText(R.id.previewEduName,     resume.getSchoolName());
        updateText(R.id.previewEduLocation, resume.getSchoolLocation());

        StringBuilder eduDesc = new StringBuilder();
        String degree  = hasValue(resume.getDegree())     ? resume.getDegree().trim()     : "";
        String eduDate = hasValue(resume.getSchoolDate()) ? resume.getSchoolDate().trim() : "";
        if (!degree.isEmpty() || !eduDate.isEmpty()) {
            eduDesc.append(degree);
            if (!degree.isEmpty() && !eduDate.isEmpty()) eduDesc.append("    ");
            eduDesc.append(eduDate);
        }
        appendBullets(eduDesc, resume.getSchoolDescription());
        updateText(R.id.previewEduDesc, eduDesc.toString().trim());

        // ── Experience ────────────────────────────────────────────────────────
        StringBuilder expHeader = new StringBuilder();
        if (hasValue(resume.getExpOrgName())) expHeader.append(resume.getExpOrgName().trim());
        String pos     = hasValue(resume.getExpPosition()) ? resume.getExpPosition().trim() : "";
        String expDate = hasValue(resume.getExpDate())     ? resume.getExpDate().trim()     : "";
        if (!pos.isEmpty() || !expDate.isEmpty()) {
            expHeader.append("\n").append(pos);
            if (!pos.isEmpty() && !expDate.isEmpty()) expHeader.append("    ");
            expHeader.append(expDate);
        }
        updateText(R.id.previewExpName, expHeader.toString().trim());

        StringBuilder expBullets = new StringBuilder();
        appendBullets(expBullets, resume.getExpBullets());
        updateText(R.id.previewExpDesc, expBullets.toString().trim());

        // ── Volunteering ──────────────────────────────────────────────────────
        StringBuilder volHeader = new StringBuilder();
        if (hasValue(resume.getVolOrgName())) volHeader.append(resume.getVolOrgName().trim());
        String volPos  = hasValue(resume.getVolPosition()) ? resume.getVolPosition().trim() : "";
        String volDate = hasValue(resume.getVolDate())     ? resume.getVolDate().trim()     : "";
        if (!volPos.isEmpty() || !volDate.isEmpty()) {
            volHeader.append("\n").append(volPos);
            if (!volPos.isEmpty() && !volDate.isEmpty()) volHeader.append("    ");
            volHeader.append(volDate);
        }
        updateText(R.id.previewVolName, volHeader.toString().trim());

        StringBuilder volBullets = new StringBuilder();
        appendBullets(volBullets, resume.getVolBullets());
        updateText(R.id.previewVolDesc, volBullets.toString().trim());

        // ── Projects ──────────────────────────────────────────────────────────
        StringBuilder projHeader = new StringBuilder();
        if (hasValue(resume.getProjectName())) projHeader.append(resume.getProjectName().trim());
        String projRole = hasValue(resume.getProjectRole()) ? resume.getProjectRole().trim() : "";
        String projDate = hasValue(resume.getProjectDate()) ? resume.getProjectDate().trim() : "";
        if (!projRole.isEmpty() || !projDate.isEmpty()) {
            projHeader.append("\n").append(projRole);
            if (!projRole.isEmpty() && !projDate.isEmpty()) projHeader.append("    ");
            projHeader.append(projDate);
        }
        updateText(R.id.previewProjectName, projHeader.toString().trim());

        StringBuilder projBullets = new StringBuilder();
        appendBullets(projBullets, resume.getProjectBullets());
        updateText(R.id.previewProjectDesc, projBullets.toString().trim());

        // ── Languages ─────────────────────────────────────────────────────────
        updateText(R.id.previewLanguages, resume.getLanguages() != null
                ? resume.getLanguages() : "");

        // ── Skills ────────────────────────────────────────────────────────────
        updateText(R.id.previewSkills, resume.getSkills() != null
                ? resume.getSkills() : "");
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    private void generatePDF() {
        ScrollView scrollView = findViewById(R.id.resumeCaptureArea);
        if (scrollView == null || scrollView.getChildAt(0) == null) {
            Toast.makeText(this, "Preview not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        View content     = scrollView.getChildAt(0);
        int  totalWidth  = scrollView.getWidth();
        int  totalHeight = content.getHeight();

        if (totalWidth == 0 || totalHeight == 0) {
            Toast.makeText(this, "Preview not ready — please wait", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas bitmapCanvas = new Canvas(bitmap);
        bitmapCanvas.drawColor(Color.WHITE);
        content.draw(bitmapCanvas);

        int pdfWidth  = 595;
        int pdfHeight = (int) ((float) totalHeight / totalWidth * pdfWidth);

        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);

        Matrix scale = new Matrix();
        scale.setScale((float) pdfWidth / totalWidth, (float) pdfWidth / totalWidth);
        page.getCanvas().drawBitmap(bitmap, scale, null);

        doc.finishPage(page);
        bitmap.recycle();

        File file = new File(getExternalFilesDir(null), "SkillSpark_Resume.pdf");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            doc.writeTo(fos);
            Toast.makeText(this, "PDF saved!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write PDF", e);
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
        } finally {
            doc.close();
        }
    }

    private void sharePDF() {
        File file = new File(getExternalFilesDir(null), "SkillSpark_Resume.pdf");
        if (!file.exists()) generatePDF();
        if (!file.exists()) {
            Toast.makeText(this, "Could not create PDF to share", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share Resume"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null) tv.setText(text != null ? text : "");
    }

    private boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        if (!hasValue(value)) return;
        if (sb.length() > 0) sb.append(" | ");
        sb.append(value.trim());
    }

    private void appendBullets(StringBuilder sb, String raw) {
        if (!hasValue(raw)) return;
        for (String line : raw.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (sb.length() > 0) sb.append("\n");
            if (trimmed.startsWith("•") || trimmed.startsWith("-")) {
                sb.append(trimmed);
            } else {
                sb.append("• ").append(trimmed);
            }
        }
    }
}