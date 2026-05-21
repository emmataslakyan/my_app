package com.example.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SplitResultActivity extends BaseActivity {

    private File        folderPath;
    private File        pendingDownload;

    private final ActivityResultLauncher<Intent> saveFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK
                        && result.getData() != null
                        && pendingDownload != null) {
                    writeToUri(result.getData().getData(), pendingDownload);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_result);
        setupCommonToolbar();

        String path  = getIntent().getStringExtra("FOLDER_PATH");
        String title = getIntent().getStringExtra("TITLE");
        folderPath   = new File(path);

        TextView tvTitle = findViewById(R.id.tvTitle);
        if (title != null) tvTitle.setText(title);

        List<File> parts = loadParts();

        RecyclerView rv = findViewById(R.id.rvParts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        PartAdapter adapter = new PartAdapter(parts, this::onOpenPart, this::onEditPart, this::onSharePart, this::onDownloadPart);
        rv.setAdapter(adapter);
    }

    private List<File> loadParts() {
        File[] files = folderPath.listFiles(f -> f.getName().endsWith(".pdf"));
        if (files == null) return new ArrayList<>();
        Arrays.sort(files, Comparator.comparing(File::getName));
        return new ArrayList<>(Arrays.asList(files));
    }

    private void onOpenPart(File file) {
        Intent intent = new Intent(this, MergedPdfPreviewActivity.class);
        intent.putExtra("LOCAL_PATH", file.getAbsolutePath());
        intent.putExtra("FILE_NAME", file.getName().replace(".pdf", ""));
        startActivity(intent);
    }

    private void onEditPart(File file, PartAdapter adapter, int position) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        // Pre-fill with current display name (strip the page range suffix and .pdf)
        String current = file.getName().replace(".pdf", "");
        int pi = current.indexOf(" (pages ");
        String pagesSuffix = pi != -1 ? current.substring(pi) : "";
        input.setText(pi != -1 ? current.substring(0, pi) : current);
        input.setSelection(input.getText().length());

        FrameLayout container = new FrameLayout(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Rename part")
                .setView(container)
                .setPositiveButton("Rename", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) return;
                    newName = newName.replaceAll("[\\\\/:*?\"<>|]", "_");
                    File renamed = new File(file.getParentFile(), newName + pagesSuffix + ".pdf");
                    if (file.renameTo(renamed)) {
                        adapter.updateFile(position, renamed);
                    } else {
                        Toast.makeText(this, "Could not rename file", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onSharePart(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share " + file.getName()));
    }

    private void onDownloadPart(File file) {
        pendingDownload = file;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, file.getName());
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        saveFileLauncher.launch(intent);
    }

    private void writeToUri(Uri destination, File src) {
        new Thread(() -> {
            try (InputStream in  = new FileInputStream(src);
                 OutputStream out = getContentResolver().openOutputStream(destination)) {
                if (out == null) throw new IOException("Cannot open output stream");
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                runOnUiThread(() -> Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // ── Adapter ────────────────────────────────────────────────────────────

    private static class PartAdapter extends RecyclerView.Adapter<PartAdapter.VH> {
        interface OnOpen     { void onOpen(File f); }
        interface OnEdit     { void onEdit(File f, PartAdapter adapter, int position); }
        interface OnShare    { void onShare(File f); }
        interface OnDownload { void onDownload(File f); }

        private final List<File> items;
        private final OnOpen     onOpen;
        private final OnEdit     onEdit;
        private final OnShare    onShare;
        private final OnDownload onDownload;

        PartAdapter(List<File> items, OnOpen onOpen, OnEdit onEdit, OnShare onShare, OnDownload onDownload) {
            this.items      = items;
            this.onOpen     = onOpen;
            this.onEdit     = onEdit;
            this.onShare    = onShare;
            this.onDownload = onDownload;
        }

        void updateFile(int position, File newFile) {
            items.set(position, newFile);
            notifyItemChanged(position);
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_split_result, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            File file = items.get(position);
            String fullName = file.getName().replace(".pdf", "");

            // Parse embedded range from filename "Name (pages X-Y)"
            String part  = fullName;
            String range = "";
            int pi = fullName.indexOf(" (pages ");
            if (pi != -1) {
                part  = fullName.substring(0, pi);
                range = "Pages " + fullName.substring(pi + 8, fullName.lastIndexOf(')'));
            }

            h.tvPartName.setText(part);
            h.tvPartRange.setText(range);
            h.itemView.setOnClickListener(v -> onOpen.onOpen(file));
            h.btnEdit.setOnClickListener(v -> onEdit.onEdit(file, this, h.getAdapterPosition()));
            h.btnShare.setOnClickListener(v -> onShare.onShare(file));
            h.btnDownload.setOnClickListener(v -> onDownload.onDownload(file));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView    tvPartName, tvPartRange;
            final ImageButton btnEdit, btnShare, btnDownload;
            VH(View v) {
                super(v);
                tvPartName  = v.findViewById(R.id.tvPartName);
                tvPartRange = v.findViewById(R.id.tvPartRange);
                btnEdit     = v.findViewById(R.id.btnPartEdit);
                btnShare    = v.findViewById(R.id.btnPartShare);
                btnDownload = v.findViewById(R.id.btnPartDownload);
            }
        }
    }
}
