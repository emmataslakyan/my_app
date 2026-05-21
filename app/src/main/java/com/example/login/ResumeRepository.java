package com.example.login;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Firestore-backed CRUD for {@link Resume} documents at
 * {@code users/<uid>/resumes/<id>}. All asynchronous methods run callbacks on the
 * main thread for UI convenience. The {@link #getBlocking(String)} helper is for
 * background-thread render paths (preview/view).
 */
public final class ResumeRepository {

    public interface OnSuccess<T> { void onSuccess(@NonNull T result); }
    public interface OnError { void onError(@NonNull String message); }

    private static final String COLLECTION = "resumes";

    private final FirebaseFirestore db;

    public ResumeRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Nullable
    private CollectionReference resumesCollection() {
        String uid = currentUid();
        if (uid == null) return null;
        return db.collection("users").document(uid).collection(COLLECTION);
    }

    @Nullable
    private static String currentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /** Persists a new resume, returning the assigned document id. */
    public void create(@NonNull Resume resume, @NonNull OnSuccess<String> onSuccess, @NonNull OnError onError) {
        CollectionReference col = resumesCollection();
        if (col == null) { onError.onError("Not signed in"); return; }
        DocumentReference ref = col.document();
        resume.setId(ref.getId());
        ref.set(resume)
                .addOnSuccessListener(v -> onSuccess.onSuccess(ref.getId()))
                .addOnFailureListener(e -> onError.onError(safeMessage(e)));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update(@NonNull Resume resume, @NonNull Runnable onSuccess, @NonNull OnError onError) {
        CollectionReference col = resumesCollection();
        if (col == null) { onError.onError("Not signed in"); return; }
        if (resume.getId() == null || resume.getId().isEmpty()) {
            onError.onError("Resume has no id"); return;
        }
        col.document(resume.getId()).set(resume)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.onError(safeMessage(e)));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public void get(@NonNull String id, @NonNull OnSuccess<Resume> onSuccess, @NonNull OnError onError) {
        CollectionReference col = resumesCollection();
        if (col == null) { onError.onError("Not signed in"); return; }
        col.document(id).get()
                .addOnSuccessListener(doc -> {
                    Resume r = toResume(doc);
                    if (r == null) onError.onError("Resume not found");
                    else onSuccess.onSuccess(r);
                })
                .addOnFailureListener(e -> onError.onError(safeMessage(e)));
    }

    public void getAll(@NonNull OnSuccess<List<Resume>> onSuccess, @NonNull OnError onError) {
        CollectionReference col = resumesCollection();
        if (col == null) { onError.onError("Not signed in"); return; }
        col.get()
                .addOnSuccessListener(snap -> {
                    List<Resume> out = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Resume r = toResume(doc);
                        if (r != null) out.add(r);
                    }
                    onSuccess.onSuccess(out);
                })
                .addOnFailureListener(e -> onError.onError(safeMessage(e)));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(@NonNull String id, @NonNull Runnable onSuccess, @NonNull OnError onError) {
        CollectionReference col = resumesCollection();
        if (col == null) { onError.onError("Not signed in"); return; }
        col.document(id).delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.onError(safeMessage(e)));
    }

    // ── Blocking helper for worker-thread render paths ────────────────────────

    /** Blocks the calling thread (max 5 s) waiting for Firestore. Returns null on failure. */
    @Nullable
    public Resume getBlocking(@NonNull String id) {
        final Resume[] holder = {null};
        final CountDownLatch latch = new CountDownLatch(1);
        get(id,
                r -> { holder[0] = r; latch.countDown(); },
                err -> latch.countDown());
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return holder[0];
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nullable
    private static Resume toResume(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Resume r = doc.toObject(Resume.class);
        if (r != null) r.setId(doc.getId());
        return r;
    }

    @NonNull
    private static String safeMessage(@Nullable Exception e) {
        return e == null || e.getMessage() == null ? "Unknown error" : e.getMessage();
    }
}
