package com.example.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.Map;

public class ProfileEditActivity extends BaseActivity {

    private UserProfileManager profileManager;

    private EditText etFullName;
    private EditText etPhone;
    private EditText etAddress;
    private EditText etLinkedin;
    private EditText etWebsite;
    private EditText etSummary;
    private EditText etNationality;
    private EditText etDob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        profileManager = new UserProfileManager();

        bindViews();
        loadCurrentValues();

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Button btnSave = findViewById(R.id.btn_save_profile);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveProfile());
        }
    }

    // -------------------------------------------------------------------------
    // View binding
    // -------------------------------------------------------------------------

    private void bindViews() {
        etFullName    = findViewById(R.id.et_full_name);
        etPhone       = findViewById(R.id.et_phone);
        etAddress     = findViewById(R.id.et_address);
        etLinkedin    = findViewById(R.id.et_linkedin);
        etWebsite     = findViewById(R.id.et_website);
        etSummary     = findViewById(R.id.et_summary);
        etNationality = findViewById(R.id.et_nationality);
        etDob         = findViewById(R.id.et_dob);
    }

    // -------------------------------------------------------------------------
    // Load existing values from Firestore
    // -------------------------------------------------------------------------

    private void loadCurrentValues() {
        profileManager.loadProfile(data -> runOnUiThread(() -> {
            setEditText(etFullName,    data, UserProfileManager.KEY_FULL_NAME);
            setEditText(etPhone,       data, UserProfileManager.KEY_PHONE);
            setEditText(etAddress,     data, UserProfileManager.KEY_ADDRESS);
            setEditText(etLinkedin,    data, UserProfileManager.KEY_LINKEDIN);
            setEditText(etWebsite,     data, UserProfileManager.KEY_WEBSITE);
            setEditText(etSummary,     data, UserProfileManager.KEY_SUMMARY);
            setEditText(etNationality, data, UserProfileManager.KEY_NATIONALITY);
            setEditText(etDob,         data, UserProfileManager.KEY_DOB);
        }), err -> {});
    }

    private void setEditText(EditText et, Map<String, Object> data, String key) {
        if (et == null) return;
        Object v = data.get(key);
        if (v != null) et.setText(v.toString());
    }

    // -------------------------------------------------------------------------
    // Save to Firestore
    // -------------------------------------------------------------------------

    private void saveProfile() {
        profileManager.saveProfile(
                trim(etFullName),
                trim(etPhone),
                trim(etAddress),
                trim(etLinkedin),
                trim(etWebsite),
                trim(etSummary),
                trim(etNationality),
                trim(etDob),
                () -> runOnUiThread(() -> {
                    Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
                    finish();
                }),
                err -> runOnUiThread(() ->
                        Toast.makeText(this, "Save failed: " + err,
                                Toast.LENGTH_LONG).show()));
    }

    private String trim(EditText et) {
        return et != null ? et.getText().toString().trim() : "";
    }
}