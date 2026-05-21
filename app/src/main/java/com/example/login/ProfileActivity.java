package com.example.login;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";
    /** Sentinel resume id used by section editors to switch into "profile mode" (writes to UserProfileManager instead of a resume). */
    public static final String PROFILE_RESUME_ID = "__profile__";

    private FirebaseAuth mAuth;
    private UserProfileManager profileManager;
    private LinearLayout containerEducation;
    private LinearLayout containerExperience;
    private LinearLayout containerSkills;
    private LinearLayout containerLanguages;
    private LinearLayout containerDocuments;
    private TextView tvEduEmpty;
    private TextView tvExpEmpty;
    private TextView tvSkillsEmpty;
    private TextView tvLanguagesEmpty;
    private TextView tvDocsEmpty;
    private ImageView profileAvatar;

    private ActivityResultLauncher<String> documentPickerLauncher;
    private ActivityResultLauncher<String> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth          = FirebaseAuth.getInstance();
        profileManager = new UserProfileManager();

        bindViews();
        setupToolbar();
        setupButtons();
        setupPickers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileHeader();
        loadPersonalInfoSummary();
        loadEducationSection();
        loadExperienceSection();
        loadSkillsSection();
        loadLanguagesSection();
        loadDocumentsSection();
        loadProfilePhoto();
    }

    private void bindViews() {
        containerEducation  = findViewById(R.id.container_education);
        containerExperience = findViewById(R.id.container_experience);
        containerSkills     = findViewById(R.id.container_skills);
        containerLanguages  = findViewById(R.id.container_languages);
        containerDocuments  = findViewById(R.id.container_documents);
        tvEduEmpty          = findViewById(R.id.tv_edu_empty);
        tvExpEmpty          = findViewById(R.id.tv_exp_empty);
        tvSkillsEmpty       = findViewById(R.id.tv_skills_empty);
        tvLanguagesEmpty    = findViewById(R.id.tv_languages_empty);
        tvDocsEmpty         = findViewById(R.id.tv_docs_empty);
        profileAvatar       = findViewById(R.id.profile_avatar);
    }

    private void setupToolbar() {
        ImageView btnBack = findViewById(R.id.btn_back);
        ImageView btnLang = findViewById(R.id.btn_language_menu);
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        if (btnLang != null) btnLang.setOnClickListener(v -> showLanguageSheet());
    }

    private void setupButtons() {
        View btnMyResumes = findViewById(R.id.btn_my_resumes);
        if (btnMyResumes != null)
            btnMyResumes.setOnClickListener(v -> startActivity(new Intent(this, MyResumesActivity.class)));

        ImageButton btnEditPersonal = findViewById(R.id.btn_edit_profile);
        if (btnEditPersonal != null)
            btnEditPersonal.setOnClickListener(v -> {
                Intent i = new Intent(this, PersonalDetailsActivity.class);
                i.putExtra("RESUME_ID", PROFILE_RESUME_ID);
                startActivity(i);
            });

        ImageButton btnEditEdu = findViewById(R.id.btn_edit_education);
        if (btnEditEdu != null)
            btnEditEdu.setOnClickListener(v -> {
                Intent i = new Intent(this, EducationListActivity.class);
                i.putExtra("RESUME_ID", PROFILE_RESUME_ID);
                startActivity(i);
            });

        ImageButton btnEditExp = findViewById(R.id.btn_edit_experience);
        if (btnEditExp != null)
            btnEditExp.setOnClickListener(v -> {
                Intent i = new Intent(this, ExperienceListActivity.class);
                i.putExtra("RESUME_ID", PROFILE_RESUME_ID);
                startActivity(i);
            });

        ImageButton btnEditSkills = findViewById(R.id.btn_edit_skills);
        if (btnEditSkills != null)
            btnEditSkills.setOnClickListener(v -> {
                Intent i = new Intent(this, SkillsActivity.class);
                i.putExtra("RESUME_ID", PROFILE_RESUME_ID);
                startActivity(i);
            });

        ImageButton btnEditLanguages = findViewById(R.id.btn_edit_languages);
        if (btnEditLanguages != null)
            btnEditLanguages.setOnClickListener(v -> {
                Intent i = new Intent(this, LanguagesActivity.class);
                i.putExtra("RESUME_ID", PROFILE_RESUME_ID);
                startActivity(i);
            });

        ImageButton btnUpload = findViewById(R.id.btn_upload_document);
        if (btnUpload != null)
            btnUpload.setOnClickListener(v -> documentPickerLauncher.launch("application/pdf"));

        if (profileAvatar != null)
            profileAvatar.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));

        ImageView btnChangePhoto = findViewById(R.id.btn_change_photo);
        if (btnChangePhoto != null)
            btnChangePhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));

        View btnLogout = findViewById(R.id.logout_button);
        if (btnLogout != null)
            btnLogout.setOnClickListener(v -> performLogout());
    }

    private void setupPickers() {
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) handleProfilePhotoSelected(uri); });

        documentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) uploadDocument(uri); });
    }

    // ── Document Upload Logic ────────────────────────────────────────────────

    private void uploadDocument(Uri fileUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { toast("Not signed in"); return; }

        String raw = getFileName(fileUri);
        final String fileName = (raw != null && !raw.isEmpty())
                ? raw : "certificate_" + System.currentTimeMillis() + ".pdf";

        final String mimeType = getContentResolver().getType(fileUri);
        final String storageName = System.currentTimeMillis() + "_" + fileName;

        java.io.InputStream stream;
        try {
            stream = getContentResolver().openInputStream(fileUri);
        } catch (Exception e) {
            toast("Could not open file.");
            return;
        }
        if (stream == null) { toast("Could not read the file."); return; }

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(mimeType != null ? mimeType : "application/pdf")
                .setCustomMetadata("originalName", fileName)
                .build();

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("users/" + user.getUid() + "/documents/" + storageName);

        toast("Uploading " + fileName + "...");

        storageRef.putStream(stream, metadata)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            Log.d(TAG, "Upload success: " + downloadUri);
                            profileManager.saveDocumentEntry(
                                    fileName,
                                    downloadUri.toString(),
                                    "pdf",
                                    () -> runOnUiThread(() -> {
                                        toast("Certificate saved!");
                                        loadDocumentsSection();
                                    }),
                                    err -> runOnUiThread(() -> toast("Save failed: " + err)));
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload failed", e);
                    runOnUiThread(() -> toast("Upload failed: " + e.getLocalizedMessage()));
                });
    }

    private void deleteDocument(String docId) {
        profileManager.deleteDocumentEntry(docId,
                () -> runOnUiThread(() -> {
                    toast("Certificate removed.");
                    loadDocumentsSection();
                }),
                err -> runOnUiThread(() -> toast("Delete failed: " + err)));
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result;
    }

    // ── Profile Photo Logic ──────────────────────────────────────────────────
    //
    // Source of truth: Firebase Storage (bytes) + Firestore "photoUrl" (download URL).
    // We do NOT persist a local cache — Glide's URL cache handles fast subsequent loads.

    private void handleProfilePhotoSelected(Uri uri) {
        // Show the picked image immediately as a preview while the upload runs.
        if (profileAvatar != null) {
            profileAvatar.setImageTintList(null);
            Glide.with(this).load(uri).circleCrop().into(profileAvatar);
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        byte[] photoBytes = readBytesFromUri(uri);
        if (photoBytes == null) {
            toast("Couldn't read the selected image.");
            return;
        }

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("users/" + user.getUid() + "/profile_photo.jpg");

        ref.putBytes(photoBytes)
                .addOnSuccessListener(snap ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUri ->
                                profileManager.savePhotoUrl(
                                        downloadUri.toString(), () -> {}, err ->
                                                Log.e(TAG, "Photo URL save failed: " + err))))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Profile photo upload failed: " + e.getMessage()));
    }

    private byte[] readBytesFromUri(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[4096];
            int n;
            while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);
            return buf.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "readBytesFromUri failed", e);
            return null;
        }
    }

    private void loadProfilePhoto() {
        profileManager.loadProfile(data -> runOnUiThread(() -> {
            String url = getStr(data, UserProfileManager.KEY_PHOTO_URL);
            if (url.isEmpty() || profileAvatar == null) return;
            profileAvatar.setImageTintList(null);
            Glide.with(this).load(url).circleCrop().into(profileAvatar);
        }), err -> Log.e(TAG, "Photo load failed: " + err));
    }

    // ── Data Loading & UI ─────────────────────────────────────────────────────

    private void loadProfileHeader() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        TextView tvName  = findViewById(R.id.profile_username);
        TextView tvEmail = findViewById(R.id.profile_email);
        if (tvName != null)
            tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Your Name");
        if (tvEmail != null) tvEmail.setText(user.getEmail());
    }

    private void loadPersonalInfoSummary() {
        profileManager.loadProfile(data -> runOnUiThread(() -> {
            String fullName = getStr(data, UserProfileManager.KEY_FULL_NAME);
            if (!fullName.isEmpty()) {
                TextView tvUsername = findViewById(R.id.profile_username);
                if (tvUsername != null) tvUsername.setText(fullName);
            }
            setFieldText(R.id.tv_name,       fullName);
            setFieldText(R.id.tv_email_info, getStr(data, UserProfileManager.KEY_EMAIL));
            setFieldText(R.id.tv_phone,      getStr(data, UserProfileManager.KEY_PHONE));
            setFieldText(R.id.tv_address,    getStr(data, UserProfileManager.KEY_ADDRESS));
            setFieldText(R.id.tv_linkedin,   getStr(data, UserProfileManager.KEY_LINKEDIN));

            String summary = getStr(data, UserProfileManager.KEY_SUMMARY);
            TextView tvSummary = findViewById(R.id.tv_summary);
            if (tvSummary != null) {
                tvSummary.setText(summary);
                tvSummary.setVisibility(summary.isEmpty() ? View.GONE : View.VISIBLE);
            }
        }), err -> Log.e(TAG, "Load profile error: " + err));
    }

    private void setFieldText(int viewId, String value) {
        TextView tv = findViewById(viewId);
        if (tv != null) tv.setText(value.isEmpty() ? "—" : value);
    }

    private void loadEducationSection() {
        profileManager.loadEducation(list -> runOnUiThread(() -> {
            containerEducation.removeAllViews();
            tvEduEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            for (Map<String, Object> entry : list) {
                containerEducation.addView(buildEducationRow(entry));
            }
        }), err -> Log.e(TAG, "Load education error: " + err));
    }

    private void loadExperienceSection() {
        profileManager.loadExperience(list -> runOnUiThread(() -> {
            containerExperience.removeAllViews();
            tvExpEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            for (Map<String, Object> entry : list) {
                containerExperience.addView(buildExperienceRow(entry));
            }
        }), err -> Log.e(TAG, "Load experience error: " + err));
    }

    private void loadSkillsSection() {
        profileManager.loadProfile(data -> runOnUiThread(() -> {
            containerSkills.removeAllViews();
            Object v = data.get(UserProfileManager.KEY_SKILLS);
            String stored = v != null ? v.toString() : "";
            if (stored.isEmpty()) {
                tvSkillsEmpty.setVisibility(View.VISIBLE);
                return;
            }
            tvSkillsEmpty.setVisibility(View.GONE);
            for (String skillEntry : stored.split("\\|")) {
                if (skillEntry.trim().isEmpty()) continue;
                containerSkills.addView(buildSkillRow(skillEntry));
            }
        }), err -> Log.e(TAG, "Load skills error: " + err));
    }

    private View buildExperienceRow(Map<String, Object> entry) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(0, 0, 0, dpToPx(12));

        String org  = getStr(entry, UserProfileManager.EXP_ORG);
        String pos  = getStr(entry, UserProfileManager.EXP_POS);
        String date = getStr(entry, UserProfileManager.EXP_DATE);

        TextView tvOrg = new TextView(this);
        tvOrg.setText(org.isEmpty() ? "—" : org);
        tvOrg.setTextColor(0xFF444444);
        tvOrg.setTextSize(14);
        card.addView(tvOrg);

        if (!pos.isEmpty()) {
            TextView tvPos = new TextView(this);
            tvPos.setText(pos);
            tvPos.setTextColor(0xFF666666);
            tvPos.setTextSize(13);
            card.addView(tvPos);
        }

        if (!date.isEmpty()) {
            TextView tvDate = new TextView(this);
            tvDate.setText(date);
            tvDate.setTextColor(0xFF888888);
            tvDate.setTextSize(12);
            card.addView(tvDate);
        }

        return card;
    }

    private void loadLanguagesSection() {
        profileManager.loadProfile(data -> runOnUiThread(() -> {
            containerLanguages.removeAllViews();
            Object v = data.get(UserProfileManager.KEY_LANGUAGES);
            String stored = v != null ? v.toString() : "";
            if (stored.isEmpty()) {
                tvLanguagesEmpty.setVisibility(View.VISIBLE);
                return;
            }
            tvLanguagesEmpty.setVisibility(View.GONE);
            for (String langEntry : stored.split("\\|")) {
                if (langEntry.trim().isEmpty()) continue;
                containerLanguages.addView(buildLanguageRow(langEntry));
            }
        }), err -> Log.e(TAG, "Load languages error: " + err));
    }

    private View buildLanguageRow(String langEntry) {
        String[] parts = langEntry.split(":", 2);
        String name  = parts.length > 0 ? parts[0].trim() : langEntry;
        String level = parts.length > 1 ? parts[1].trim() : "";

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dpToPx(6));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(0xFF444444);
        tvName.setTextSize(14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(lp);
        row.addView(tvName);

        if (!level.isEmpty()) {
            TextView tvLevel = new TextView(this);
            tvLevel.setText(level);
            tvLevel.setTextColor(0xFF888888);
            tvLevel.setTextSize(12);
            row.addView(tvLevel);
        }

        return row;
    }

    private View buildSkillRow(String skillEntry) {
        String[] parts = skillEntry.split(":", 2);
        String name  = parts.length > 0 ? parts[0].trim() : skillEntry;
        String level = parts.length > 1 ? parts[1].trim() : "";

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dpToPx(6));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(0xFF444444);
        tvName.setTextSize(14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(lp);
        row.addView(tvName);

        if (!level.isEmpty()) {
            TextView tvLevel = new TextView(this);
            tvLevel.setText(level);
            tvLevel.setTextColor(0xFF888888);
            tvLevel.setTextSize(12);
            row.addView(tvLevel);
        }

        return row;
    }

    private void loadDocumentsSection() {
        profileManager.loadDocuments(list -> runOnUiThread(() -> {
            containerDocuments.removeAllViews();
            tvDocsEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            for (Map<String, Object> entry : list) {
                containerDocuments.addView(buildDocumentRow(entry));
            }
        }), err -> Log.e(TAG, "Load docs error: " + err));
    }

    private View buildDocumentRow(Map<String, Object> entry) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_document_row, containerDocuments, false);
        TextView tvName = row.findViewById(R.id.tv_doc_name);
        String name  = getStr(entry, "name");
        String docId = getStr(entry, UserProfileManager.DOC_ID);
        tvName.setText(name.isEmpty() ? "Certificate" : name);
        row.setOnClickListener(v -> {
            String url = getStr(entry, "url");
            if (!url.isEmpty()) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
        android.widget.ImageButton btnDel = row.findViewById(R.id.btn_doc_delete);
        if (btnDel != null && !docId.isEmpty()) {
            btnDel.setOnClickListener(v -> deleteDocument(docId));
        }
        return row;
    }

    private View buildEducationRow(Map<String, Object> entry) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(0, 0, 0, dpToPx(12));

        TextView tv = new TextView(this);
        String degree = getStr(entry, UserProfileManager.EDU_DEGREE);
        String school = getStr(entry, UserProfileManager.EDU_SCHOOL);
        String text = degree + " - " + school;
        tv.setText(text);
        tv.setTextColor(0xFF444444);
        card.addView(tv);

        return card;
    }

    // ── Navigation & Utils ───────────────────────────────────────────────────

    private void showLanguageSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.layout_language_sheet, null);
        if (v != null) {
            View en = v.findViewById(R.id.btn_select_en);
            View ru = v.findViewById(R.id.btn_select_ru);
            if (en != null) en.setOnClickListener(view -> { setLocale("en"); sheet.dismiss(); });
            if (ru != null) ru.setOnClickListener(view -> { setLocale("ru"); sheet.dismiss(); });
            sheet.setContentView(v);
            sheet.show();
        }
    }

    private void setLocale(String lang) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(lang);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }

    private void performLogout() {
        mAuth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String getStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
