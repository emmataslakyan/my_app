package com.example.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class PersonalDetailsActivity extends BaseActivity {

    private static final String TAG = "PersonalDetailsActivity";

    // RESUME_ID == -1 means "profile mode" — saves to Firestore, not Room DB
    private static final int PROFILE_RESUME_ID = ProfileActivity.PROFILE_RESUME_ID;

    private TextInputEditText editName, editEmail, editPhone, editAddress,
            editLinkedin, editSummary;
    private ShapeableImageView imgProfilePhoto;

    private AppDatabase db;
    private int currentResumeId;
    private Resume currentResume;
    private String pendingPhotoPath = null;

    private UserProfileManager profileManager;
    private boolean isProfileMode;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            handlePhotoSelected(result.getData().getData());
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        currentResumeId = getIntent().getIntExtra("RESUME_ID", -1);
        isProfileMode   = (currentResumeId == PROFILE_RESUME_ID);
        db              = AppDatabase.getInstance(this);
        profileManager  = new UserProfileManager();

        bindViews();

        // Apply placeholder tint in code (so it only affects the placeholder, not photos)
        imgProfilePhoto.setImageTintList(
                android.content.res.ColorStateList.valueOf(0xFF46186E));

        imgProfilePhoto.setOnClickListener(v -> openImagePicker());
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnInstructions).setOnClickListener(v -> showInstructionsDialog());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveDetails());

        loadExistingData();
    }

    // ── Bind ──────────────────────────────────────────────────────────────────

    private void bindViews() {
        imgProfilePhoto = findViewById(R.id.imgProfilePhoto);
        editName        = findViewById(R.id.editName);
        editEmail       = findViewById(R.id.editEmail);
        editPhone       = findViewById(R.id.editPhone);
        editAddress     = findViewById(R.id.editAddress);
        editLinkedin    = findViewById(R.id.editLinkedin);
    }

    // ── Image picker ──────────────────────────────────────────────────────────

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        imagePickerLauncher.launch(intent);
    }

    private void handlePhotoSelected(Uri sourceUri) {
        String fileName = isProfileMode
                ? "profile_photo_global.jpg"
                : "profile_photo_" + currentResumeId + ".jpg";

        // Copy to internal storage so we always have a local path
        String savedPath = copyImageToInternalStorage(sourceUri, fileName);
        if (savedPath == null) {
            Toast.makeText(this, "Failed to load image. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        pendingPhotoPath = savedPath;
        loadBitmapIntoView(savedPath);   // shows the photo immediately

        if (isProfileMode) {
            // Cache path for ProfileActivity to find it quickly
            getSharedPreferences("ProfilePrefs", MODE_PRIVATE)
                    .edit().putString("profile_photo_path", savedPath).apply();

            // Upload to Firebase Storage for cross-device sync
            uploadPhotoToFirebase(sourceUri);
        }
    }

    // ── Firebase Storage photo upload ─────────────────────────────────────────

    private void uploadPhotoToFirebase(Uri fileUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Read into bytes first — avoids URI permission issues with Storage SDK
        byte[] photoBytes = readBytesFromUri(fileUri);
        if (photoBytes == null) {
            Log.e(TAG, "Could not read photo bytes for upload");
            return;
        }

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("users/" + user.getUid() + "/profile_photo.jpg");

        ref.putBytes(photoBytes)
                .addOnSuccessListener(snap ->
                        ref.getDownloadUrl().addOnSuccessListener(uri ->
                                profileManager.savePhotoUrl(uri.toString(), () -> {}, err ->
                                        Log.e(TAG, "Photo URL Firestore save failed: " + err))))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Profile photo upload failed: " + e.getMessage()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String copyImageToInternalStorage(Uri sourceUri, String fileName) {
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

    /**
     * Loads a bitmap and CLEARS the ImageView tint first so the photo
     * is not coloured by the placeholder tint.
     */
    private void loadBitmapIntoView(String path) {
        if (path == null || imgProfilePhoto == null) return;
        File f = new File(path);
        if (!f.exists()) return;
        Bitmap bmp = BitmapFactory.decodeFile(path);
        if (bmp == null) return;

        // MUST clear tint before setting bitmap — otherwise the purple tint
        // is applied over the photo making it invisible
        imgProfilePhoto.setImageTintList(null);
        imgProfilePhoto.setImageBitmap(bmp);
    }

    // ── Load data ─────────────────────────────────────────────────────────────

    private void loadExistingData() {
        if (isProfileMode) {
            // Read from Firestore
            profileManager.loadProfile(data -> runOnUiThread(() -> {
                setField(editName,     getStr(data, UserProfileManager.KEY_FULL_NAME));
                setField(editEmail,    getStr(data, UserProfileManager.KEY_EMAIL));
                setField(editPhone,    getStr(data, UserProfileManager.KEY_PHONE));
                setField(editAddress,  getStr(data, UserProfileManager.KEY_ADDRESS));
                setField(editLinkedin, getStr(data, UserProfileManager.KEY_LINKEDIN));
                setField(editSummary,  getStr(data, UserProfileManager.KEY_SUMMARY));

                // Load photo from local cache
                String localPath = getSharedPreferences("ProfilePrefs", MODE_PRIVATE)
                        .getString("profile_photo_path", null);
                if (localPath != null) {
                    pendingPhotoPath = localPath;
                    loadBitmapIntoView(localPath);
                }
            }), err -> Log.e(TAG, "Firestore load failed: " + err));

        } else {
            // Per-resume mode — read from Room DB
            if (currentResumeId < 0) return;
            new Thread(() -> {
                currentResume = db.resumeDao().getResumeById(currentResumeId);
                if (currentResume == null) return;
                runOnUiThread(() -> {
                    setField(editName,    currentResume.getName());
                    setField(editEmail,   currentResume.getEmail());
                    setField(editPhone,   currentResume.getPhone());
                    setField(editAddress, currentResume.getAddress());
                    // LinkedIn / summary are in the resume if you add those columns;
                    // pre-filled values come from the Firestore snapshot via applySnapshotToPrefs

                    String savedPath = currentResume.getPhotoPath();
                    if (savedPath != null) {
                        pendingPhotoPath = savedPath;
                        loadBitmapIntoView(savedPath);
                    }
                });
            }).start();
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void saveDetails() {
        if (isProfileMode) saveToFirestore();
        else               saveToRoomDb();
    }

    private void saveToFirestore() {
        // Save all personal fields
        profileManager.saveProfile(
                trimField(editName),
                trimField(editPhone),
                trimField(editAddress),
                trimField(editLinkedin),
                "",
                trimField(editSummary),
                "", "",
                () -> {
                    // Also save email (separate key)
                    profileManager.saveExtraField(
                            UserProfileManager.KEY_EMAIL, trimField(editEmail),
                            () -> runOnUiThread(() -> {
                                Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
                                finish();
                            }),
                            err -> runOnUiThread(() ->
                                    Toast.makeText(this, "Saved but email failed: " + err,
                                            Toast.LENGTH_SHORT).show()));
                },
                err -> runOnUiThread(() ->
                        Toast.makeText(this, "Save failed: " + err, Toast.LENGTH_LONG).show()));
    }

    private void saveToRoomDb() {
        if (currentResume == null) return;
        currentResume.setName(trimField(editName));
        currentResume.setEmail(trimField(editEmail));
        currentResume.setPhone(trimField(editPhone));
        currentResume.setAddress(trimField(editAddress));
        if (pendingPhotoPath != null) currentResume.setPhotoPath(pendingPhotoPath);

        new Thread(() -> {
            db.resumeDao().update(currentResume);
            runOnUiThread(() -> {
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    // ── Instructions ──────────────────────────────────────────────────────────

    private void showInstructionsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Quick Tips")
                .setMessage(
                        "• Tap your photo to upload a new one.\n" +
                                "• Use a professional email address.\n" +
                                "• Double-check your phone number.\n" +
                                "• Address is usually city and country.\n" +
                                "• LinkedIn and Summary auto-fill every new resume.")
                .setPositiveButton("Got it", (d, w) -> d.dismiss())
                .show();
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void setField(TextInputEditText et, String value) {
        if (et != null && value != null && !value.isEmpty()) et.setText(value);
    }

    private String trimField(TextInputEditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    private String getStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}