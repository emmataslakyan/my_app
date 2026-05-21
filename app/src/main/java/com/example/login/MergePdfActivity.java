package com.example.login;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MergePdfActivity extends BaseActivity {

    private static final File[] EMPTY_FILES = new File[0];

    private final List<Uri>  selectedFiles  = new ArrayList<>();
    private final List<File> historyFiles   = new ArrayList<>();

    private PdfFileAdapter   fileAdapter;
    private HistoryAdapter   historyAdapter;

    private View           emptyState;
    private View           historySection;
    private MaterialButton btnMerge;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                Intent data = result.getData();
                if (data.getClipData() != null) {
                    ClipData clip = data.getClipData();
                    for (int i = 0; i < clip.getItemCount(); i++) addUri(clip.getItemAt(i).getUri());
                } else if (data.getData() != null) {
                    addUri(data.getData());
                }
                updateFileUi();
            });

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_pdf);
        setupCommonToolbar();

        emptyState     = findViewById(R.id.emptyState);
        historySection = findViewById(R.id.historySection);
        btnMerge       = findViewById(R.id.btnMerge);

        RecyclerView rvFiles = findViewById(R.id.rvPdfFiles);
        rvFiles.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new PdfFileAdapter(selectedFiles, this::onRemoveFile);
        rvFiles.setAdapter(fileAdapter);

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(historyFiles, this::onOpenHistory, this::onDeleteHistory);
        rvHistory.setAdapter(historyAdapter);

        findViewById(R.id.btnAddFiles).setOnClickListener(v -> openFilePicker());
        btnMerge.setOnClickListener(v -> startMerge());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    // ── File picker ────────────────────────────────────────────────────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select PDFs"));
    }

    private void addUri(Uri uri) {
        if (!selectedFiles.contains(uri)) {
            selectedFiles.add(uri);
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {}
        }
    }

    private void onRemoveFile(int position) {
        selectedFiles.remove(position);
        fileAdapter.notifyItemRemoved(position);
        updateFileUi();
    }

    private void updateFileUi() {
        boolean hasFiles = !selectedFiles.isEmpty();
        emptyState.setVisibility(hasFiles ? View.GONE : View.VISIBLE);
        btnMerge.setEnabled(selectedFiles.size() >= 2);
        fileAdapter.notifyDataSetChanged();
    }

    // ── History ────────────────────────────────────────────────────────────

    private File historyDir() {
        File dir = new File(getFilesDir(), "merge_history");
        dir.mkdirs();
        return dir;
    }

    private void loadHistory() {
        File[] files = historyDir().listFiles(f -> f.getName().endsWith(".pdf"));
        if (files == null) files = EMPTY_FILES;
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        historyFiles.clear();
        for (File f : files) historyFiles.add(f);
        historyAdapter.notifyDataSetChanged();
        historySection.setVisibility(historyFiles.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void onOpenHistory(File file) {
        String name = file.getName().replace(".pdf", "");
        launchPreview(file, name);
    }

    private void onDeleteHistory(int position) {
        File file = historyFiles.get(position);
        file.delete();
        historyFiles.remove(position);
        historyAdapter.notifyItemRemoved(position);
        historySection.setVisibility(historyFiles.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ── Merge flow ─────────────────────────────────────────────────────────

    private void startMerge() {
        setButtonState("Merging…", false);

        new Thread(() -> {
            File tmp = new File(getCacheDir(), "tmp_merge_" + System.currentTimeMillis() + ".pdf");
            String error = mergePdfs(tmp);

            if (error != null) {
                runOnUiThread(() -> {
                    setButtonState("Merge", true);
                    Toast.makeText(this, "Merge failed: " + error, Toast.LENGTH_LONG).show();
                });
                return;
            }

            runOnUiThread(() -> showRenameDialog(tmp));
        }).start();
    }

    private void showRenameDialog(File tmpFile) {
        setButtonState("Merge", selectedFiles.size() >= 2);

        EditText input = new EditText(this);
        input.setHint("e.g. Project Report");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        FrameLayout container = new FrameLayout(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Name your merged PDF")
                .setView(container)
                .setPositiveButton("Save", (d, w) -> {
                    String raw  = input.getText().toString().trim();
                    String name = raw.isEmpty()
                            ? "merged_" + System.currentTimeMillis()
                            : raw.replaceAll("[\\\\/:*?\"<>|]", "_");
                    saveToHistory(tmpFile, name);
                })
                .setNegativeButton("Use default", (d, w) ->
                        saveToHistory(tmpFile, "merged_" + System.currentTimeMillis()))
                .setCancelable(false)
                .show();
    }

    private void saveToHistory(File tmpFile, String name) {
        setButtonState("Saving…", false);

        new Thread(() -> {
            File dest = new File(historyDir(), name + ".pdf");
            // If name already exists, append a counter
            int counter = 1;
            while (dest.exists()) {
                dest = new File(historyDir(), name + " (" + counter++ + ").pdf");
            }
            File finalDest = dest;

            try (InputStream in = new FileInputStream(tmpFile);
                 OutputStream out = new FileOutputStream(finalDest)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                tmpFile.delete();
            } catch (IOException e) {
                finalDest = tmpFile; // fallback: use temp file
            }

            final File histFile  = finalDest;
            final String display = histFile.getName().replace(".pdf", "");

            runOnUiThread(() -> {
                setButtonState("Uploading…", false);
                uploadToFirebase(histFile, display);
            });
        }).start();
    }

    private void uploadToFirebase(File localFile, String displayName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid  = user != null ? user.getUid() : "anon";
        String path = "merged_pdfs/" + uid + "/" + localFile.getName();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(path);
        ref.putFile(Uri.fromFile(localFile))
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Upload failed — file saved locally", Toast.LENGTH_SHORT).show();
                    }
                    launchPreview(localFile, displayName);
                });
    }

    private void launchPreview(File localFile, String displayName) {
        setButtonState("Merge", selectedFiles.size() >= 2);
        Intent intent = new Intent(this, MergedPdfPreviewActivity.class);
        intent.putExtra("LOCAL_PATH", localFile.getAbsolutePath());
        intent.putExtra("FILE_NAME", displayName);
        startActivity(intent);
    }

    private void setButtonState(String label, boolean enabled) {
        btnMerge.setText(label);
        btnMerge.setEnabled(enabled);
    }

    // ── PDF merge logic ────────────────────────────────────────────────────

    private String mergePdfs(File output) {
        PdfDocument mergedDoc = new PdfDocument();
        int pageNumber = 1;
        final int SCALE = 3; // ~216 DPI
        try {
            for (Uri uri : selectedFiles) {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                if (pfd == null) continue;

                PdfRenderer renderer = new PdfRenderer(pfd);
                for (int i = 0; i < renderer.getPageCount(); i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    int nativeW = page.getWidth();
                    int nativeH = page.getHeight();

                    Bitmap bmp = Bitmap.createBitmap(nativeW * SCALE, nativeH * SCALE, Bitmap.Config.ARGB_8888);
                    bmp.eraseColor(Color.WHITE);
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                    page.close();

                    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(nativeW, nativeH, pageNumber++).create();
                    PdfDocument.Page outPage = mergedDoc.startPage(info);
                    android.graphics.Matrix m = new android.graphics.Matrix();
                    m.setScale(1f / SCALE, 1f / SCALE);
                    outPage.getCanvas().drawBitmap(bmp, m, null);
                    mergedDoc.finishPage(outPage);
                    bmp.recycle();
                }
                renderer.close();
                pfd.close();
            }

            try (FileOutputStream fos = new FileOutputStream(output)) {
                mergedDoc.writeTo(fos);
            }
            return null;
        } catch (IOException e) {
            return e.getMessage();
        } finally {
            mergedDoc.close();
        }
    }

    // ── Selected-files adapter ─────────────────────────────────────────────

    private static class PdfFileAdapter extends RecyclerView.Adapter<PdfFileAdapter.VH> {
        interface OnRemove { void onRemove(int position); }
        private final List<Uri> items;
        private final OnRemove  onRemove;

        PdfFileAdapter(List<Uri> items, OnRemove onRemove) { this.items = items; this.onRemove = onRemove; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_file, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Uri    uri  = items.get(position);
            String name = uri.getLastPathSegment();
            if (name != null && name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);
            h.tvFileName.setText(name != null ? name : uri.toString());
            h.btnRemove.setOnClickListener(v -> onRemove.onRemove(h.getAdapterPosition()));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView    tvFileName;
            final ImageButton btnRemove;
            VH(View v) { super(v); tvFileName = v.findViewById(R.id.tvFileName); btnRemove = v.findViewById(R.id.btnRemove); }
        }
    }

    // ── History adapter ────────────────────────────────────────────────────

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        interface OnOpen   { void onOpen(File file); }
        interface OnDelete { void onDelete(int position); }

        private final List<File> items;
        private final OnOpen     onOpen;
        private final OnDelete   onDelete;
        private final SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault());

        HistoryAdapter(List<File> items, OnOpen onOpen, OnDelete onDelete) {
            this.items    = items;
            this.onOpen   = onOpen;
            this.onDelete = onDelete;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_pdf, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            File file = items.get(position);
            h.tvName.setText(file.getName().replace(".pdf", ""));
            h.tvDate.setText(sdf.format(new Date(file.lastModified())));
            h.itemView.setOnClickListener(v -> onOpen.onOpen(file));
            h.btnDelete.setOnClickListener(v -> onDelete.onDelete(h.getAdapterPosition()));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView    tvName, tvDate;
            final ImageButton btnDelete;
            VH(View v) {
                super(v);
                tvName    = v.findViewById(R.id.tvHistoryName);
                tvDate    = v.findViewById(R.id.tvHistoryDate);
                btnDelete = v.findViewById(R.id.btnHistoryDelete);
            }
        }
    }
}
