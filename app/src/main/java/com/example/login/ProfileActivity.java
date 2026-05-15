package com.example.login;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";
    public static final int PROFILE_RESUME_ID = -1;

    private FirebaseAuth mAuth;
    private UserProfileManager profileManager;
    private LinearLayout containerEducation;
    private LinearLayout containerDocuments;
    private TextView tvEduEmpty;
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
        loadDocumentsSection();
        loadProfilePhoto();
    }

    private void bindViews() {
        containerEducation = findViewById(R.id.container_education);
        containerDocuments = findViewById(R.id.container_documents);
        tvEduEmpty         = findViewById(R.id.tv_edu_empty);
        tvDocsEmpty        = findViewById(R.id.tv_docs_empty);
        profileAvatar      = findViewById(R.id.profile_avatar);
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
                Intent i = new Intent(this, EducationActivity.class);
                i.putExtra("RESUME_ID", PROFILE_RESUME_ID);
                startActivity(i);
            });

        ImageButton btnUpload = findViewById(R.id.btn_upload_document);
        if (btnUpload != null)
            btnUpload.setOnClickListener(v -> documentPickerLauncher.launch("*/*"));

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

        final String fileName = getFileName(fileUri);
        final String mimeType = getContentResolver().getType(fileUri);
        final String type = (mimeType != null && mimeType.contains("pdf")) ? "pdf" : "file";

        byte[] fileBytes = readBytesFromUri(fileUri);
        if (fileBytes == null) {
            toast("Could not read the file.");
            return;
        }

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("originalName", fileName)
                .build();

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("users/" + user.getUid() + "/documents/" + fileName);

        toast("Uploading " + fileName + "...");

        storageRef.putBytes(fileBytes, metadata)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            Log.d(TAG, "Upload success: " + downloadUri);
                            profileManager.saveDocumentEntry(
                                    fileName,
                                    downloadUri.toString(),
                                    type,
                                    () -> runOnUiThread(() -> {
                                        toast("Document saved!");
                                        loadDocumentsSection();
                                    }),
                                    err -> toast("Metadata sync failed: " + err));
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase Upload Error", e);
                    toast("Upload failed: " + e.getLocalizedMessage());
                });
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

    // ── Profile Photo Logic ──────────────────────────────────────────────────

    private void handleProfilePhotoSelected(Uri uri) {
        String path = copyImageToStorage(uri, "profile_photo_global.jpg");
        if (path != null) {
            getSharedPreferences("ProfilePrefs", MODE_PRIVATE)
                    .edit().putString("profile_photo_path", path).apply();
            loadBitmapIntoAvatar(path);
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        byte[] bytes = readBytesFromUri(uri);
        if (bytes == null) return;

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("users/" + user.getUid() + "/profile_photo.jpg");

        ref.putBytes(bytes)
                .addOnSuccessListener(snap ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUri ->
                                profileManager.savePhotoUrl(
                                        downloadUri.toString(), () -> {}, err ->
                                                Log.e(TAG, "Photo URL save failed: " + err))))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Profile photo upload failed: " + e.getMessage()));
    }

    private String copyImageToStorage(Uri sourceUri, String fileName) {
        try {
            File destFile = new File(getFilesDir(), fileName);
            try (InputStream in  = getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(destFile)) {
                if (in == null) return null;
                Bitmap bmp = BitmapFactory.decodeStream(in);
                if (bmp == null) return null;
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy image", e);
            return null;
        }
    }

    private void loadProfilePhoto() {
        String localPath = getSharedPreferences("ProfilePrefs", MODE_PRIVATE)
                .getString("profile_photo_path", null);
        if (localPath != null && new File(localPath).exists()) {
            loadBitmapIntoAvatar(localPath);
        }
    }

    private void loadBitmapIntoAvatar(String path) {
        if (path == null || profileAvatar == null) return;
        Bitmap bmp = BitmapFactory.decodeFile(path);
        if (bmp != null) {
            profileAvatar.setImageTintList(null);
            profileAvatar.setImageBitmap(bmp);
        }
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
            setFieldText(R.id.tv_name,       getStr(data, UserProfileManager.KEY_FULL_NAME));
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
        String name = getStr(entry, "name");
        tvName.setText(name);
        row.setOnClickListener(v -> {
            String url = getStr(entry, "url");
            if (!url.isEmpty()) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
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
