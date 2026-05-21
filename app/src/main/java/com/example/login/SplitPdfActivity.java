package com.example.login;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SplitPdfActivity extends BaseActivity {

    private static final File[] EMPTY_FILES = new File[0];

    private Uri    selectedUri;
    private int    totalPages;

    private final List<File> historyFolders = new ArrayList<>();
    private HistoryAdapter   historyAdapter;

    private View           emptyState;
    private View           historySection;
    private View           fileInfoCard;
    private View           rangesSection;
    private TextView       tvFileName;
    private TextView       tvPageCount;
    private TextView       tvRangeHint;
    private EditText       etRanges;
    private MaterialButton btnSplit;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                Uri uri = result.getData().getData();
                if (uri != null) loadFile(uri);
            });

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_pdf);
        setupCommonToolbar();

        emptyState    = findViewById(R.id.emptyState);
        historySection = findViewById(R.id.historySection);
        fileInfoCard  = findViewById(R.id.fileInfoCard);
        rangesSection = findViewById(R.id.rangesSection);
        tvFileName    = findViewById(R.id.tvFileName);
        tvPageCount   = findViewById(R.id.tvPageCount);
        tvRangeHint   = findViewById(R.id.tvRangeHint);
        etRanges      = findViewById(R.id.etRanges);
        btnSplit      = findViewById(R.id.btnSplit);

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(historyFolders, this::onOpenHistory, this::onDeleteHistory);
        rvHistory.setAdapter(historyAdapter);

        findViewById(R.id.btnSelectFile).setOnClickListener(v -> openFilePicker());
        btnSplit.setOnClickListener(v -> startSplit());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    // ── File selection ─────────────────────────────────────────────────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select PDF to split"));
    }

    private void loadFile(Uri uri) {
        new Thread(() -> {
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                if (pfd == null) return;
                PdfRenderer renderer = new PdfRenderer(pfd);
                int pages = renderer.getPageCount();
                renderer.close();
                pfd.close();

                String raw  = uri.getLastPathSegment();
                String name = (raw != null && raw.contains("/"))
                        ? raw.substring(raw.lastIndexOf('/') + 1)
                        : (raw != null ? raw : "document.pdf");

                runOnUiThread(() -> onFileLoaded(uri, name, pages));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Could not read PDF: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void onFileLoaded(Uri uri, String name, int pages) {
        selectedUri = uri;
        totalPages  = pages;

        tvFileName.setText(name);
        tvPageCount.setText(pages + (pages == 1 ? " page" : " pages"));
        tvRangeHint.setText("Total: " + pages + " page" + (pages == 1 ? "" : "s") + " — enter ranges below");

        // Suggest a default split at the midpoint if more than 1 page
        if (pages > 1) {
            int mid = pages / 2;
            etRanges.setText("1-" + mid + ", " + (mid + 1) + "-" + pages);
        } else {
            etRanges.setText("1-1");
        }

        emptyState.setVisibility(View.GONE);
        fileInfoCard.setVisibility(View.VISIBLE);
        rangesSection.setVisibility(View.VISIBLE);
        btnSplit.setEnabled(true);
    }

    // ── History ────────────────────────────────────────────────────────────

    private File historyRoot() {
        File dir = new File(getFilesDir(), "split_history");
        dir.mkdirs();
        return dir;
    }

    private void loadHistory() {
        File[] folders = historyRoot().listFiles(File::isDirectory);
        if (folders == null) folders = EMPTY_FILES;
        Arrays.sort(folders, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        historyFolders.clear();
        for (File f : folders) historyFolders.add(f);
        historyAdapter.notifyDataSetChanged();
        historySection.setVisibility(historyFolders.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void onOpenHistory(File folder) {
        Intent intent = new Intent(this, SplitResultActivity.class);
        intent.putExtra("FOLDER_PATH", folder.getAbsolutePath());
        intent.putExtra("TITLE", folder.getName());
        startActivity(intent);
    }

    private void onDeleteHistory(int position) {
        File folder = historyFolders.get(position);
        deleteFolder(folder);
        historyFolders.remove(position);
        historyAdapter.notifyItemRemoved(position);
        historySection.setVisibility(historyFolders.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) for (File f : files) f.delete();
        folder.delete();
    }

    // ── Split flow ─────────────────────────────────────────────────────────

    private void startSplit() {
        String rangeInput = etRanges.getText() != null ? etRanges.getText().toString().trim() : "";
        List<int[]> ranges = parseRanges(rangeInput, totalPages);

        if (ranges.isEmpty()) {
            Toast.makeText(this, "Enter at least one valid range (e.g. 1-5)", Toast.LENGTH_SHORT).show();
            return;
        }

        setButtonState("Splitting…", false);

        new Thread(() -> {
            String error = performSplit(selectedUri, ranges);
            if (error != null) {
                runOnUiThread(() -> {
                    setButtonState("Split", true);
                    Toast.makeText(this, "Split failed: " + error, Toast.LENGTH_LONG).show();
                });
                return;
            }
            runOnUiThread(() -> saveToHistory("split_" + System.currentTimeMillis(), ranges));
        }).start();
    }

    // Temporary split files stored in cache; moved to history after naming
    private List<File> tempParts;

    private String performSplit(Uri uri, List<int[]> ranges) {
        tempParts = new ArrayList<>();
        File tmpDir = new File(getCacheDir(), "split_tmp_" + System.currentTimeMillis());
        tmpDir.mkdirs();

        final int SCALE = 3;
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) return "Cannot open file";
            PdfRenderer renderer = new PdfRenderer(pfd);

            for (int r = 0; r < ranges.size(); r++) {
                int fromPage = ranges.get(r)[0] - 1; // 0-indexed
                int toPage   = ranges.get(r)[1] - 1;

                File part = new File(tmpDir, "Part " + (r + 1) + ".pdf");
                PdfDocument doc = new PdfDocument();
                int outNum = 1;

                for (int i = fromPage; i <= toPage; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    int nativeW = page.getWidth();
                    int nativeH = page.getHeight();

                    Bitmap bmp = Bitmap.createBitmap(nativeW * SCALE, nativeH * SCALE, Bitmap.Config.ARGB_8888);
                    bmp.eraseColor(Color.WHITE);
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                    page.close();

                    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(nativeW, nativeH, outNum++).create();
                    PdfDocument.Page outPage = doc.startPage(info);
                    android.graphics.Matrix m = new android.graphics.Matrix();
                    m.setScale(1f / SCALE, 1f / SCALE);
                    outPage.getCanvas().drawBitmap(bmp, m, null);
                    doc.finishPage(outPage);
                    bmp.recycle();
                }

                try (FileOutputStream fos = new FileOutputStream(part)) {
                    doc.writeTo(fos);
                }
                doc.close();
                tempParts.add(part);
            }

            renderer.close();
            pfd.close();
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    private void saveToHistory(String baseName, List<int[]> ranges) {
        setButtonState("Saving…", false);

        new Thread(() -> {
            // Ensure unique folder name
            File dest = new File(historyRoot(), baseName);
            int counter = 1;
            while (dest.exists()) {
                dest = new File(historyRoot(), baseName + " (" + counter++ + ")");
            }
            dest.mkdirs();
            final File opFolder = dest;

            // Move temp parts into the named folder, embed range in filename
            List<File> finalParts = new ArrayList<>();
            for (int i = 0; i < tempParts.size(); i++) {
                String label = "Part " + (i + 1)
                        + " (pages " + ranges.get(i)[0] + "-" + ranges.get(i)[1] + ").pdf";
                File dst = new File(opFolder, label);
                tempParts.get(i).renameTo(dst);
                finalParts.add(dst);
            }

            uploadToFirebase(opFolder, finalParts, opFolder.getName());
        }).start();
    }

    private void uploadToFirebase(File opFolder, List<File> parts, String displayName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid  = user != null ? user.getUid() : "anon";
        String base = "split_pdfs/" + uid + "/" + displayName + "/";

        StorageReference root = FirebaseStorage.getInstance().getReference();
        int[] pending = {parts.size()};

        for (File part : parts) {
            StorageReference ref = root.child(base + part.getName());
            ref.putFile(Uri.fromFile(part)).addOnCompleteListener(task -> {
                pending[0]--;
                if (pending[0] == 0) launchResult(opFolder, displayName);
            });
        }

        if (parts.isEmpty()) launchResult(opFolder, displayName);
    }

    private void launchResult(File opFolder, String displayName) {
        runOnUiThread(() -> {
            setButtonState("Split", btnSplit.isEnabled() || selectedUri != null);
            Intent intent = new Intent(this, SplitResultActivity.class);
            intent.putExtra("FOLDER_PATH", opFolder.getAbsolutePath());
            intent.putExtra("TITLE", displayName);
            startActivity(intent);
        });
    }

    private void setButtonState(String label, boolean enabled) {
        btnSplit.setText(label);
        btnSplit.setEnabled(enabled);
    }

    // ── Range parser ───────────────────────────────────────────────────────

    private List<int[]> parseRanges(String input, int pageCount) {
        List<int[]> result = new ArrayList<>();
        if (input.isEmpty()) return result;

        for (String part : input.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            String[] bounds = part.split("-");
            if (bounds.length != 2) continue;
            try {
                int from = Integer.parseInt(bounds[0].trim());
                int to   = bounds[1].trim().equalsIgnoreCase("end")
                        ? pageCount
                        : Integer.parseInt(bounds[1].trim());
                if (from >= 1 && to <= pageCount && from <= to) {
                    result.add(new int[]{from, to});
                } else {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Range " + from + "-" + to + " is out of bounds (1-" + pageCount + ")",
                            Toast.LENGTH_SHORT).show());
                }
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    // ── History adapter ────────────────────────────────────────────────────

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        interface OnOpen   { void onOpen(File folder); }
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
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_split, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            File folder = items.get(position);
            File[] parts = folder.listFiles(f -> f.getName().endsWith(".pdf"));
            int count = parts != null ? parts.length : 0;
            h.tvName.setText(folder.getName());
            h.tvMeta.setText(count + " part" + (count == 1 ? "" : "s") + " · " + sdf.format(new Date(folder.lastModified())));
            h.itemView.setOnClickListener(v -> onOpen.onOpen(folder));
            h.btnDelete.setOnClickListener(v -> onDelete.onDelete(h.getAdapterPosition()));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView    tvName, tvMeta;
            final ImageButton btnDelete;
            VH(View v) {
                super(v);
                tvName    = v.findViewById(R.id.tvHistoryName);
                tvMeta    = v.findViewById(R.id.tvHistoryMeta);
                btnDelete = v.findViewById(R.id.btnHistoryDelete);
            }
        }
    }
}
