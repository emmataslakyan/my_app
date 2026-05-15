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

    public static final String EDU_DEGREE = "degree";
    public static final String EDU_SCHOOL = "school";
    public static final String EDU_FIELD  = "fieldOfStudy";
    public static final String EDU_START  = "startYear";
    public static final String EDU_END    = "endYear";
    public static final String EDU_GPA    = "gpa";
    public static final String EDU_ID     = "id";

    public static final String DOC_NAME     = "name";
    public static final String DOC_URL      = "url";
    public static final String DOC_TYPE     = "type";
    public static final String DOC_UPLOADED = "uploadedAt";
    public static final String DOC_ID       = "id";

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
                                   String degree, String school, String field,
                                   String startYear, String endYear, String gpa,
                                   OnSuccessCallback onSuccess, OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        Map<String, Object> entry = new HashMap<>();
        entry.put(EDU_DEGREE, degree);
        entry.put(EDU_SCHOOL, school);
        entry.put(EDU_FIELD,  field);
        entry.put(EDU_START,  startYear);
        entry.put(EDU_END,    endYear);
        entry.put(EDU_GPA,    gpa);
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
        documentsRef()
                .orderBy(DOC_UPLOADED,
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
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

    // ── Resume auto-population ────────────────────────────────────────────────

    public void populateNewResume(int resumeId,
                                  OnSuccessCallback onDone, OnFailureCallback onFailure) {
        if (uid == null) { onFailure.onFailure("User not signed in"); return; }
        loadProfile(profileData -> {
            Map<String, Object> snapshot = new HashMap<>(profileData);
            loadEducation(eduList -> {
                snapshot.put("educationSnapshot", eduList);
                snapshot.put("createdAt", com.google.firebase.Timestamp.now());
                writeResumeSnapshot(resumeId, snapshot, onDone, onFailure);
            }, err -> writeResumeSnapshot(resumeId, snapshot, onDone, onFailure));
        }, err -> onFailure.onFailure(err));
    }

    private void writeResumeSnapshot(int resumeId, Map<String, Object> snapshot,
                                     OnSuccessCallback onDone, OnFailureCallback onFailure) {
        db.collection("resumes").document(String.valueOf(resumeId))
                .set(snapshot, SetOptions.merge())
                .addOnSuccessListener(v -> onDone.onSuccess())
                .addOnFailureListener(e -> onFailure.onFailure(e.getMessage()));
    }

    public void applySnapshotToPrefs(android.content.Context ctx, int resumeId,
                                     OnSuccessCallback onDone) {
        db.collection("resumes").document(String.valueOf(resumeId)).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { onDone.onSuccess(); return; }
                    android.content.SharedPreferences.Editor prefs =
                            ctx.getSharedPreferences("PersonalDetails_" + resumeId,
                                    android.content.Context.MODE_PRIVATE).edit();
                    safeSet(prefs, "fullName", doc.getString(KEY_FULL_NAME));
                    safeSet(prefs, "email",    doc.getString(KEY_EMAIL));
                    safeSet(prefs, "phone",    doc.getString(KEY_PHONE));
                    safeSet(prefs, "address",  doc.getString(KEY_ADDRESS));
                    safeSet(prefs, "linkedin", doc.getString(KEY_LINKEDIN));
                    safeSet(prefs, "summary",  doc.getString(KEY_SUMMARY));
                    prefs.apply();
                    onDone.onSuccess();
                })
                .addOnFailureListener(e -> onDone.onSuccess());
    }

    private void safeSet(android.content.SharedPreferences.Editor prefs,
                         String key, String value) {
        if (value != null && !value.isEmpty()) prefs.putString(key, value);
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────

    public interface OnSuccessCallback { void onSuccess(); }
    public interface OnFailureCallback { void onFailure(String error); }
    public interface OnDataCallback<T> { void onData(T data); }
}