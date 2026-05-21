package com.example.login;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfileManager {

    // ── Field keys ────────────────────────────────────────────────────────────
    public static final String KEY_FULL_NAME   = "fullName";
    public static final String KEY_EMAIL       = "email";
    public static final String KEY_PHONE       = "phone";
    public static final String KEY_ADDRESS     = "address";
    public static final String KEY_LINKEDIN    = "linkedin";
    public static final String KEY_WEBSITE     = "website";
    public static final String KEY_SUMMARY     = "summary";
    public static final String KEY_NATIONALITY = "nationality";
    public static final String KEY_DOB         = "dateOfBirth";
    public static final String KEY_PHOTO_URL   = "photoUrl";

    public static final String EDU_SCHOOL      = "school";
    public static final String EDU_LOCATION    = "location";
    public static final String EDU_DATE        = "date";
    public static final String EDU_DEGREE      = "degree";
    public static final String EDU_DESCRIPTION = "description";
    public static final String EDU_ID          = "id";

    public static final String DOC_NAME     = "name";
    public static final String DOC_URL      = "url";
    public static final String DOC_TYPE     = "type";
    public static final String DOC_UPLOADED = "uploadedAt";
    public static final String DOC_ID       = "id";

    public static final String EXP_ORG     = "orgName";
    public static final String EXP_POS     = "position";
    public static final String EXP_LOC     = "location";
    public static final String EXP_DATE    = "date";
    public static final String EXP_BULLETS = "bullets";
    public static final String EXP_ID      = "id";

    public static final String KEY_SKILLS     = "skills";
    public static final String KEY_LANGUAGES  = "languages";

    private final FirebaseFirestore db;
    private final String uid;

    public UserProfileManager() {
        db  = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    // ── Refs ──────────────────────────────────────────────────────────────────

    private DocumentReference profileRef() {
        return db.collection("users").document(uid)
                .collection("data").document("profile");
    }

    private com.google.firebase.firestore.CollectionReference educationRef() {
        return db.collection("users").document(uid).collection("education");
    }

    private com.google.firebase.firestore.CollectionReference documentsRef() {
        return db.collection("users").document(uid).collection("documents");
    }

    private com.google.firebase.firestore.CollectionReference experienceRef() {
        return db.collection("users").document(uid).collection("experience");
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    public void saveProfile(String fullName, String phone, String address,
                            String linkedin, String website, String summary,
                            String nationality, String dob,
                            OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        Map<String, Object> data = new HashMap<>();
        data.put(KEY_FULL_NAME,   fullName);
        data.put(KEY_PHONE,       phone);
        data.put(KEY_ADDRESS,     address);
        data.put(KEY_LINKEDIN,    linkedin);
        data.put(KEY_WEBSITE,     website);
        data.put(KEY_SUMMARY,     summary);
        data.put(KEY_NATIONALITY, nationality);
        data.put(KEY_DOB,         dob);
        profileRef().set(data, SetOptions.merge())
                .addOnSuccessListener(v -> onSuccess.onSuccess())
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    /** Merge a single field without touching anything else. */
    public void saveExtraField(String key, String value,
                               OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        Map<String, Object> data = new HashMap<>();
        data.put(key, value);
        profileRef().set(data, SetOptions.merge())
                .addOnSuccessListener(v -> onSuccess.onSuccess())
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    public void savePhotoUrl(String url, OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        saveExtraField(KEY_PHOTO_URL, url, onSuccess, onFailure);
    }

    public void loadProfile(OnDataCallback<Map<String, Object>> callback,
                            OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        profileRef().get()
                .addOnSuccessListener(doc ->
                        callback.onData(doc.exists() && doc.getData() != null
                                ? doc.getData() : new HashMap<>()))
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    // ── Education ─────────────────────────────────────────────────────────────

    public void saveEducationEntry(String entryId,
                                   String school, String location, String date,
                                   String degree, String description,
                                   OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        Map<String, Object> entry = new HashMap<>();
        entry.put(EDU_SCHOOL,      school);
        entry.put(EDU_LOCATION,    location);
        entry.put(EDU_DATE,        date);
        entry.put(EDU_DEGREE,      degree);
        entry.put(EDU_DESCRIPTION, description);
        DocumentReference ref = (entryId == null || entryId.isEmpty())
                ? educationRef().document()
                : educationRef().document(entryId);
        ref.set(entry, SetOptions.merge())
                .addOnSuccessListener(v -> onSuccess.onSuccess())
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    public void loadEducation(OnDataCallback<List<Map<String, Object>>> callback,
                              OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        educationRef().get()
                .addOnSuccessListener(query -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (var doc : query.getDocuments()) {
                        Map<String, Object> e = doc.getData();
                        if (e != null) { e.put(EDU_ID, doc.getId()); list.add(e); }
                    }
                    callback.onData(list);
                })
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    public void deleteEducationEntry(String entryId,
                                     OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        educationRef().document(entryId).delete()
                .addOnSuccessListener(v -> onSuccess.onSuccess())
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    // ── Documents ─────────────────────────────────────────────────────────────

    public void saveDocumentEntry(String name, String downloadUrl, String type,
                                  OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        Map<String, Object> doc = new HashMap<>();
        doc.put(DOC_NAME,     name);
        doc.put(DOC_URL,      downloadUrl);
        doc.put(DOC_TYPE,     type);
        doc.put(DOC_UPLOADED, com.google.firebase.Timestamp.now());
        documentsRef().add(doc)
                .addOnSuccessListener(ref -> onSuccess.onSuccess())
                .addOnFailureListener(e   -> onFailure.onFailure(e.getMessage()));
    }

    public void loadDocuments(OnDataCallback<List<Map<String, Object>>> callback,
                              OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        documentsRef().get()
                .addOnSuccessListener(query -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (var snap : query.getDocuments()) {
                        Map<String, Object> e = snap.getData();
                        if (e != null) { e.put(DOC_ID, snap.getId()); list.add(e); }
                    }
                    callback.onData(list);
                })
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    public void deleteDocumentEntry(String entryId,
                                    OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        documentsRef().document(entryId).delete()
                .addOnSuccessListener(v -> onSuccess.onSuccess())
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    // ── Experience ────────────────────────────────────────────────────────────

    public void saveExperienceEntry(String entryId, String org, String pos, String loc,
                                    String date, String bullets,
                                    OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        Map<String, Object> entry = new HashMap<>();
        entry.put(EXP_ORG, org);
        entry.put(EXP_POS, pos);
        entry.put(EXP_LOC, loc);
        entry.put(EXP_DATE, date);
        entry.put(EXP_BULLETS, bullets);
        DocumentReference ref = (entryId == null || entryId.isEmpty())
                ? experienceRef().document()
                : experienceRef().document(entryId);
        ref.set(entry, SetOptions.merge())
                .addOnSuccessListener(v -> onSuccess.onSuccess())
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    public void loadExperience(OnDataCallback<List<Map<String, Object>>> callback,
                               OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        experienceRef().get()
                .addOnSuccessListener(query -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (var doc : query.getDocuments()) {
                        Map<String, Object> e = doc.getData();
                        if (e != null) { e.put(EXP_ID, doc.getId()); list.add(e); }
                    }
                    callback.onData(list);
                })
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    public void deleteExperienceEntry(String entryId,
                                      OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        experienceRef().document(entryId).delete()
                .addOnSuccessListener(v -> onSuccess.onSuccess())
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    // ── Skills ────────────────────────────────────────────────────────────────

    public void saveSkills(String skills, OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        saveExtraField(KEY_SKILLS, skills, onSuccess, onFailure);
    }

    public void saveLanguages(String languages, OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        saveExtraField(KEY_LANGUAGES, languages, onSuccess, onFailure);
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────

    public interface OnSuccessCallback { void onSuccess(); }
    public interface OnFailureCallback { void onFailure(String error); }
    public interface OnDataCallback<T> { void onData(T data); }
}