package com.example.login;

import android.content.Intent;
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

import com.bumptech.glide.Glide;
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

    private TextInputEditText editName, editEmail, editPhone, editAddress,
            editLinkedin, editSummary;
    private ShapeableImageView imgProfilePhoto;

    private ResumeRepository repo;
    private String currentResumeId;
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

        currentResumeId = getIntent().getStringExtra("RESUME_ID");
        isProfileMode   = ProfileActivity.PROFILE_RESUME_ID.equals(currentResumeId);
        repo            = new ResumeRepository();
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
        if (isProfileMode) {
            // Profile photo: Firebase is the source of truth. Preview from the picker
            // Uri while the upload runs in the background.
            if (imgProfilePhoto != null) {
                imgProfilePhoto.setImageTintList(null);
                Glide.with(this).load(sourceUri).into(imgProfilePhoto);
            }
            uploadPhotoToFirebase(sourceUri);
            return;
        }

        // Per-resume photo override: keep a local file path so the Mustache renderer can
        // embed it as base64 even when the device is offline.
        String savedPath = copyImageToInternalStorage(
                sourceUri, "profile_photo_" + currentResumeId + ".jpg");
        if (savedPath == null) {
            Toast.makeText(this, "Failed to load image. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingPhotoPath = savedPath;
        loadBitmapIntoView(savedPath);
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
            profileManager.loadProfile(data -> runOnUiThread(() -> {
                setField(editName,     getStr(data, UserProfileManager.KEY_FULL_NAME));
                setField(editEmail,    getStr(data, UserProfileManager.KEY_EMAIL));
                setField(editPhone,    getStr(data, UserProfileManager.KEY_PHONE));
                setField(editAddress,  getStr(data, UserProfileManager.KEY_ADDRESS));
                setField(editLinkedin, getStr(data, UserProfileManager.KEY_LINKEDIN));
                setField(editSummary,  getStr(data, UserProfileManager.KEY_SUMMARY));

                String photoUrl = getStr(data, UserProfileManager.KEY_PHOTO_URL);
                if (!photoUrl.isEmpty() && imgProfilePhoto != null) {
                    imgProfilePhoto.setImageTintList(null);
                    Glide.with(this).load(photoUrl).into(imgProfilePhoto);
                }
            }), err -> Log.e(TAG, "Firestore load failed: " + err));

        } else {
            // Per-resume mode — read from Firestore
            if (currentResumeId == null || currentResumeId.isEmpty()) return;
            repo.get(currentResumeId, r -> {
                currentResume = r;
                setField(editName,    r.getName());
                setField(editEmail,   r.getEmail());
                setField(editPhone,   r.getPhone());
                setField(editAddress, r.getAddress());

                String savedPath = r.getPhotoPath();
                if (savedPath != null && !savedPath.isEmpty()) {
                    displayPhotoFromPathOrUrl(savedPath);
                } else {
                    loadProfilePhotoIntoView();
                }
            }, err -> Log.e(TAG, "Resume load failed: " + err));
        }
    }

    /** Displays a stored photoPath value which may be either a local file path or an http(s) URL. */
    private void displayPhotoFromPathOrUrl(String pathOrUrl) {
        if (imgProfilePhoto == null) return;
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            imgProfilePhoto.setImageTintList(null);
            Glide.with(this).load(pathOrUrl).into(imgProfilePhoto);
        } else if (new File(pathOrUrl).exists()) {
            loadBitmapIntoView(pathOrUrl);
        }
    }

    private void loadProfilePhotoIntoView() {
        profileManager.loadProfile(data -> runOnUiThread(() -> {
            String url = getStr(data, UserProfileManager.KEY_PHOTO_URL);
            if (!url.isEmpty() && imgProfilePhoto != null) {
                imgProfilePhoto.setImageTintList(null);
                Glide.with(this).load(url).into(imgProfilePhoto);
            }
        }), err -> Log.e(TAG, "Profile photo load failed: " + err));
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void saveDetails() {
        if (isProfileMode) saveToFirestore();
        else               saveToResume();
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

    private void saveToResume() {
        if (currentResume == null) return;
        currentResume.setName(trimField(editName));
        currentResume.setEmail(trimField(editEmail));
        currentResume.setPhone(trimField(editPhone));
        currentResume.setAddress(trimField(editAddress));
        if (pendingPhotoPath != null) currentResume.setPhotoPath(pendingPhotoPath);

        repo.update(currentResume,
                () -> {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                err -> Toast.makeText(this, "Save failed: " + err, Toast.LENGTH_SHORT).show());
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