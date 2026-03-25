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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PersonalDetailsActivity extends BaseActivity {

    private static final String TAG = "PersonalDetailsActivity";

    private TextInputEditText editName, editEmail, editPhone, editAddress;
    private ShapeableImageView imgProfilePhoto;
    private AppDatabase db;
    private int currentResumeId;
    private Resume currentResume;
    private String pendingPhotoPath = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {

                            Uri sourceUri = result.getData().getData();
                            String savedPath = copyImageToInternalStorage(sourceUri);

                            if (savedPath != null) {
                                pendingPhotoPath = savedPath;
                                loadBitmapIntoView(savedPath);
                            } else {
                                Toast.makeText(this,
                                        "Failed to load image. Try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        db = AppDatabase.getInstance(this);
        currentResumeId = getIntent().getIntExtra("RESUME_ID", -1);

        // ── Bind views ────────────────────────────────────────────────────────
        imgProfilePhoto = findViewById(R.id.imgProfilePhoto);
        editName        = findViewById(R.id.editName);
        editEmail       = findViewById(R.id.editEmail);
        editPhone       = findViewById(R.id.editPhone);
        editAddress     = findViewById(R.id.editAddress);

        // ── Click listeners (no redundant casts) ──────────────────────────────
        imgProfilePhoto.setOnClickListener(v -> openImagePicker());
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnInstructions).setOnClickListener(v -> showInstructionsDialog());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveDetails());

        loadExistingData();
    }

    // ── Image picker ──────────────────────────────────────────────────────────

    private void openImagePicker() {
        // Use setDataAndType together to avoid clearing URI data
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        imagePickerLauncher.launch(intent);
    }

    private String copyImageToInternalStorage(Uri sourceUri) {
        try {
            // Unique filename per resume so multiple resumes don't clash
            String fileName = "profile_photo_" + currentResumeId + ".jpg";
            File destFile = new File(getFilesDir(), fileName);

            try (InputStream in  = getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(destFile)) {

                if (in == null) return null;

                Bitmap bitmap = BitmapFactory.decodeStream(in);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }

            return destFile.getAbsolutePath();

        } catch (Exception e) {
            // Robust logging instead of printStackTrace()
            Log.e(TAG, "Failed to copy image to internal storage", e);
            return null;
        }
    }

    private void loadBitmapIntoView(String path) {
        if (path == null) return;

        File file = new File(path);
        if (!file.exists()) return;

        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap != null) {
            imgProfilePhoto.setImageBitmap(bitmap);
        }
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    private void showInstructionsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Quick Tips")
                // Text block replaces manual concatenation
                .setMessage("""
                        • Tap your photo to upload a new one.
                        • Use a professional email address.
                        • Double-check your phone number.
                        • Address is usually city and state/country.""")
                .setPositiveButton("Got it", (d, w) -> d.dismiss())
                .show();
    }

    private void loadExistingData() {
        if (currentResumeId == -1) return;

        new Thread(() -> {
            currentResume = db.resumeDao().getResumeById(currentResumeId);
            if (currentResume != null) {
                runOnUiThread(() -> {
                    editName.setText(currentResume.getName());
                    editEmail.setText(currentResume.getEmail());
                    editPhone.setText(currentResume.getPhone());
                    editAddress.setText(currentResume.getAddress());

                    // Restore saved photo path — getPhotoPath() defined in Resume entity
                    String savedPath = currentResume.getPhotoPath();
                    if (savedPath != null) {
                        pendingPhotoPath = savedPath;
                        loadBitmapIntoView(savedPath);
                    }
                });
            }
        }).start();
    }

    private void saveDetails() {
        if (currentResume == null) return;

        // Use getText() null-safe pattern to avoid NullPointerException on toString()
        CharSequence nameText    = editName.getText();
        CharSequence emailText   = editEmail.getText();
        CharSequence phoneText   = editPhone.getText();
        CharSequence addressText = editAddress.getText();

        currentResume.setName(nameText    != null ? nameText.toString().trim()    : "");
        currentResume.setEmail(emailText  != null ? emailText.toString().trim()   : "");
        currentResume.setPhone(phoneText  != null ? phoneText.toString().trim()   : "");
        currentResume.setAddress(addressText != null ? addressText.toString().trim() : "");

        if (pendingPhotoPath != null) {
            currentResume.setPhotoPath(pendingPhotoPath);
        }

        new Thread(() -> {
            db.resumeDao().update(currentResume);
            runOnUiThread(() -> {
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}