package com.example.login;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Lists resume templates from Firestore + downloads their HTML payloads from Firebase Storage,
 * caching to {@code getCacheDir()/templates/{id}-{version}.html}. Always falls back to the
 * bundled "default" template under {@code assets/templates/default/}.
 */
public class TemplateRepository {

    private static final String TAG = "TemplateRepository";
    private static final String COLLECTION = "resume_templates";
    private static final String CACHE_SUBDIR = "templates";
    private static final long MAX_DOWNLOAD_BYTES = 5L * 1024 * 1024; // 5 MB

    public interface ListCallback {
        void onResult(List<ResumeTemplate> templates);
    }

    public interface HtmlCallback {
        void onHtml(String html, String baseUrl);
        void onError(Exception e);
    }

    private final Context appContext;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private final Executor io;

    public TemplateRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.io = Executors.newSingleThreadExecutor();
    }

    public void listTemplates(@NonNull ListCallback cb) {
        ResumeTemplate bundled = bundledDefault();
        firestore.collection(COLLECTION).get()
                .addOnSuccessListener(snap -> {
                    List<ResumeTemplate> result = new ArrayList<>();
                    result.add(bundled);
                    for (QueryDocumentSnapshot d : snap) {
                        String id = d.getId();
                        if (ResumeTemplate.DEFAULT_ID.equals(id)) continue;
                        String name = d.getString("name");
                        Long version = d.getLong("version");
                        String thumb = d.getString("thumbnail");
                        String path = d.getString("storagePath");
                        if (name == null || path == null) continue;
                        result.add(new ResumeTemplate(
                                id, name,
                                version != null ? version : 1,
                                thumb,
                                path,
                                false));
                    }
                    cb.onResult(result);
                })
                .addOnFailureListener(err -> {
                    Log.w(TAG, "Firestore list failed, returning bundled only", err);
                    cb.onResult(Collections.singletonList(bundled));
                });
    }

    public void loadTemplateHtml(@NonNull ResumeTemplate tpl, @NonNull HtmlCallback cb) {
        io.execute(() -> {
            if (tpl.isBundled() || ResumeTemplate.DEFAULT_ID.equals(tpl.getId())) {
                deliverBundled(tpl.getId(), cb);
                return;
            }
            File cached = cacheFileFor(tpl);
            if (cached.exists() && cached.length() > 0) {
                deliverFile(cached, baseUrlForRemote(tpl), cb);
                return;
            }
            String htmlPath = trimSlashes(tpl.getStoragePath()) + "/template.html";
            StorageReference ref = storage.getReference().child(htmlPath);
            ref.getBytes(MAX_DOWNLOAD_BYTES)
                    .addOnSuccessListener(bytes -> io.execute(() -> {
                        writeCache(cached, bytes);
                        deliverFile(cached, baseUrlForRemote(tpl), cb);
                    }))
                    .addOnFailureListener(err -> {
                        Log.w(TAG, "Storage download failed for " + tpl.getId() + ", using bundled", err);
                        deliverBundled(ResumeTemplate.DEFAULT_ID, cb);
                    });
        });
    }

    public ResumeTemplate bundledDefault() {
        return new ResumeTemplate(
                ResumeTemplate.DEFAULT_ID,
                "Classic",
                1,
                null,
                "assets/templates/default",
                true);
    }

    // ── internals ──────────────────────────────────────────────────────────────

    private void deliverBundled(String id, HtmlCallback cb) {
        String assetPath = "templates/" + (id == null ? ResumeTemplate.DEFAULT_ID : id) + "/template.html";
        try {
            String html = readAsset(assetPath);
            String baseUrl = "file:///android_asset/templates/"
                    + (id == null ? ResumeTemplate.DEFAULT_ID : id) + "/";
            cb.onHtml(html, baseUrl);
        } catch (IOException ioErr) {
            // Last resort: try the default
            try {
                String html = readAsset("templates/default/template.html");
                cb.onHtml(html, "file:///android_asset/templates/default/");
            } catch (IOException e2) {
                cb.onError(e2);
            }
        }
    }

    private void deliverFile(File f, String baseUrl, HtmlCallback cb) {
        try {
            String html = readFile(f);
            cb.onHtml(html, baseUrl);
        } catch (IOException e) {
            cb.onError(e);
        }
    }

    private String readAsset(String path) throws IOException {
        try (InputStream in = appContext.getAssets().open(path);
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    private String readFile(File f) throws IOException {
        try (InputStream in = new java.io.FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toString("UTF-8");
        }
    }

    private void writeCache(File target, byte[] bytes) {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(bytes);
        } catch (IOException e) {
            Log.w(TAG, "Failed to write template cache " + target, e);
        }
    }

    private File cacheFileFor(ResumeTemplate tpl) {
        File dir = new File(appContext.getCacheDir(), CACHE_SUBDIR);
        return new File(dir, tpl.getId() + "-" + tpl.getVersion() + ".html");
    }

    private String baseUrlForRemote(ResumeTemplate tpl) {
        // Templates that need bundled resources should inline CSS/images, since the cached
        // HTML lives in our private cache dir. We expose it via file:// so relative paths
        // like <img src="photo.png"> won't resolve — but absolute file:// / https:// /
        // data: URIs in the HTML work.
        return "file://" + cacheFileFor(tpl).getParent() + "/";
    }

    private static String trimSlashes(String s) {
        if (s == null) return "";
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '/') start++;
        while (end > start && s.charAt(end - 1) == '/') end--;
        return s.substring(start, end);
    }
}
