package com.example.login;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImgToPdfActivity extends BaseActivity {

    private static final int MAX_IMAGE_DIM = 1920;
    private static final File[] EMPTY_FILES = new File[0];

    private final List<Uri>  selectedUris = new ArrayList<>();
    private final List<File> historyFiles = new ArrayList<>();

    private ImageAdapter   imageAdapter;
    private HistoryAdapter historyAdapter;

    private View           emptyState;
    private View           historySection;
    private View           imagesSection;
    private TextView       tvImagesLabel;
    private MaterialButton btnConvert;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                Intent data = result.getData();
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) addUri(data.getClipData().getItemAt(i).getUri());
                } else if (data.getData() != null) {
                    addUri(data.getData());
                }
                updateUi();
            });

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_to_pdf);
        setupCommonToolbar();

        emptyState     = findViewById(R.id.emptyState);
        historySection = findViewById(R.id.historySection);
        imagesSection  = findViewById(R.id.imagesSection);
        tvImagesLabel  = findViewById(R.id.tvImagesLabel);
        btnConvert     = findViewById(R.id.btnConvert);

        RecyclerView rvImages = findViewById(R.id.rvImages);
        rvImages.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(selectedUris, this::removeImage);
        rvImages.setAdapter(imageAdapter);

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(historyFiles, this::onOpenHistory, this::onDeleteHistory);
        rvHistory.setAdapter(historyAdapter);

        findViewById(R.id.btnAddImages).setOnClickListener(v -> openFilePicker());
        btnConvert.setOnClickListener(v -> startConvert());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    // ── File picker ────────────────────────────────────────────────────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Images"));
    }

    private void addUri(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        selectedUris.add(uri);
    }

    private void removeImage(int position) {
        selectedUris.remove(position);
        imageAdapter.notifyItemRemoved(position);
        updateUi();
    }

    private void updateUi() {
        boolean hasImages = !selectedUris.isEmpty();
        emptyState.setVisibility(hasImages ? View.GONE : View.VISIBLE);
        imagesSection.setVisibility(hasImages ? View.VISIBLE : View.GONE);
        btnConvert.setEnabled(hasImages);
        int count = selectedUris.size();
        tvImagesLabel.setText(count + " image" + (count == 1 ? "" : "s") + " selected");
        imageAdapter.notifyDataSetChanged();
    }

    // ── History ────────────────────────────────────────────────────────────

    private File historyDir() {
        File dir = new File(getFilesDir(), "img_to_pdf_history");
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
        Intent intent = new Intent(this, MergedPdfPreviewActivity.class);
        intent.putExtra("LOCAL_PATH", file.getAbsolutePath());
        intent.putExtra("FILE_NAME", file.getName().replace(".pdf", ""));
        startActivity(intent);
    }

    private void onDeleteHistory(int position) {
        historyFiles.get(position).delete();
        historyFiles.remove(position);
        historyAdapter.notifyItemRemoved(position);
        historySection.setVisibility(historyFiles.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ── Convert flow ───────────────────────────────────────────────────────

    private void startConvert() {
        setButtonState("Converting…", false);
        List<Uri> uris = new ArrayList<>(selectedUris);

        new Thread(() -> {
            try {
                File temp = performConvert(uris);
                runOnUiThread(() -> showRenameDialog(temp));
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setButtonState("Convert to PDF", true);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private File performConvert(List<Uri> uris) throws IOException {
        List<byte[]> jpegs    = new ArrayList<>();
        List<int[]>  pageDims = new ArrayList<>(); // [pw, ph, iw, ih]

        for (Uri uri : uris) {
            Bitmap bmp = loadScaledBitmap(uri, MAX_IMAGE_DIM);
            if (bmp == null) continue;

            int imgW = bmp.getWidth(), imgH = bmp.getHeight();
            int pw, ph;
            if (imgW >= imgH) {
                pw = 842; ph = Math.max(1, Math.round(842f * imgH / imgW));
            } else {
                pw = 595; ph = Math.max(1, Math.round(595f * imgH / imgW));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            bmp.recycle();

            jpegs.add(baos.toByteArray());
            pageDims.add(new int[]{pw, ph, imgW, imgH});
        }

        if (jpegs.isEmpty()) throw new IOException("No images could be loaded");

        File temp = new File(getCacheDir(), "img_tmp_" + System.currentTimeMillis() + ".pdf");
        writePdf(jpegs, pageDims, temp);
        return temp;
    }

    private Bitmap loadScaledBitmap(Uri uri, int maxDim) {
        ContentResolver cr = getContentResolver();
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream in = cr.openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, opts);
            }
            int sample = 1;
            int maxSide = Math.max(opts.outWidth, opts.outHeight);
            while (maxSide / sample > maxDim) sample *= 2;
            opts.inSampleSize = sample;
            opts.inJustDecodeBounds = false;
            try (InputStream in = cr.openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void showRenameDialog(File temp) {
        EditText input = new EditText(this);
        input.setHint("File name");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        FrameLayout container = new FrameLayout(this);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Name your PDF")
                .setView(container)
                .setPositiveButton("Save", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "images_" + System.currentTimeMillis();
                    name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
                    if (!name.endsWith(".pdf")) name += ".pdf";
                    saveToHistory(temp, name);
                })
                .setNegativeButton("Cancel", (d, w) -> {
                    setButtonState("Convert to PDF", true);
                    temp.delete();
                })
                .setCancelable(false)
                .show();
    }

    private void saveToHistory(File temp, String fileName) {
        setButtonState("Saving…", false);
        new Thread(() -> {
            File dest = new File(historyDir(), fileName);
            int counter = 1;
            while (dest.exists()) {
                String base = fileName.replace(".pdf", "");
                dest = new File(historyDir(), base + " (" + counter++ + ").pdf");
            }
            final File finalDest = dest;
            temp.renameTo(finalDest);
            uploadToFirebase(finalDest);
        }).start();
    }

    private void uploadToFirebase(File localFile) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid  = user != null ? user.getUid() : "anon";
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("img_to_pdf/" + uid + "/" + localFile.getName());
        ref.putFile(Uri.fromFile(localFile))
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful())
                        Toast.makeText(this, "Upload failed — file saved locally", Toast.LENGTH_SHORT).show();
                    launchPreview(localFile);
                });
    }

    private void launchPreview(File file) {
        setButtonState("Convert to PDF", true);
        Intent intent = new Intent(this, MergedPdfPreviewActivity.class);
        intent.putExtra("LOCAL_PATH", file.getAbsolutePath());
        intent.putExtra("FILE_NAME", file.getName().replace(".pdf", ""));
        startActivity(intent);
    }

    private void setButtonState(String label, boolean enabled) {
        btnConvert.setText(label);
        btnConvert.setEnabled(enabled);
    }

    // ── PDF writer ─────────────────────────────────────────────────────────

    private void writePdf(List<byte[]> jpegs, List<int[]> pageDims, File output) throws IOException {
        int n = jpegs.size();
        List<Long> offsets = new ArrayList<>();
        long pos = 0;

        try (FileOutputStream fos = new FileOutputStream(output)) {
            pos += wr(fos, "%PDF-1.4\n");

            offsets.add(pos);
            pos += wr(fos, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            StringBuilder kids = new StringBuilder("[ ");
            for (int i = 0; i < n; i++) kids.append(3 + i * 3).append(" 0 R ");
            kids.append("]");
            offsets.add(pos);
            pos += wr(fos, "2 0 obj\n<< /Type /Pages /Kids " + kids + " /Count " + n + " >>\nendobj\n");

            for (int i = 0; i < n; i++) {
                int pw = pageDims.get(i)[0], ph = pageDims.get(i)[1];
                int iw = pageDims.get(i)[2], ih = pageDims.get(i)[3];
                byte[] jpeg = jpegs.get(i);
                int pageObj = 3 + i * 3, contObj = 4 + i * 3, imgObj = 5 + i * 3;

                offsets.add(pos);
                pos += wr(fos, pageObj + " 0 obj\n<< /Type /Page /Parent 2 0 R"
                        + " /MediaBox [0 0 " + pw + " " + ph + "]"
                        + " /Contents " + contObj + " 0 R"
                        + " /Resources << /XObject << /Im" + i + " " + imgObj + " 0 R >> >> >>\nendobj\n");

                String content = "q\n" + pw + " 0 0 " + ph + " 0 0 cm\n/Im" + i + " Do\nQ\n";
                byte[] cb = content.getBytes("ASCII");
                offsets.add(pos);
                pos += wr(fos, contObj + " 0 obj\n<< /Length " + cb.length + " >>\nstream\n");
                fos.write(cb); pos += cb.length;
                pos += wr(fos, "\nendstream\nendobj\n");

                offsets.add(pos);
                pos += wr(fos, imgObj + " 0 obj\n<< /Type /XObject /Subtype /Image"
                        + " /Width " + iw + " /Height " + ih
                        + " /ColorSpace /DeviceRGB /BitsPerComponent 8"
                        + " /Filter /DCTDecode /Length " + jpeg.length + " >>\nstream\n");
                fos.write(jpeg); pos += jpeg.length;
                pos += wr(fos, "\nendstream\nendobj\n");
            }

            long xrefOffset = pos;
            int totalObjs = 2 + n * 3;
            StringBuilder xref = new StringBuilder("xref\n0 " + (totalObjs + 1) + "\n");
            xref.append("0000000000 65535 f \n");
            for (long off : offsets) xref.append(String.format(Locale.US, "%010d 00000 n \n", off));
            wr(fos, xref.toString());
            wr(fos, "trailer\n<< /Size " + (totalObjs + 1) + " /Root 1 0 R >>\nstartxref\n"
                    + xrefOffset + "\n%%EOF\n");
        }
    }

    private long wr(FileOutputStream fos, String s) throws IOException {
        byte[] b = s.getBytes("ASCII");
        fos.write(b);
        return b.length;
    }

    // ── Image adapter ──────────────────────────────────────────────────────

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {
        interface OnRemove { void onRemove(int position); }

        private final List<Uri> items;
        private final OnRemove  onRemove;

        ImageAdapter(List<Uri> items, OnRemove onRemove) {
            this.items    = items;
            this.onRemove = onRemove;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image_thumb, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Uri uri = items.get(position);
            h.tvFileName.setText(resolveDisplayName(uri));
            h.ivThumb.setImageResource(android.R.drawable.ic_menu_gallery);
            new Thread(() -> {
                Bitmap thumb = loadScaledBitmap(uri, 128);
                if (thumb != null) h.ivThumb.post(() -> h.ivThumb.setImageBitmap(thumb));
            }).start();
            h.btnRemove.setOnClickListener(v -> onRemove.onRemove(h.getAdapterPosition()));
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            final ImageView   ivThumb;
            final TextView    tvFileName;
            final ImageButton btnRemove;
            VH(View v) {
                super(v);
                ivThumb    = v.findViewById(R.id.ivThumb);
                tvFileName = v.findViewById(R.id.tvFileName);
                btnRemove  = v.findViewById(R.id.btnRemove);
            }
        }
    }

    private String resolveDisplayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (col != -1) return c.getString(col);
            }
        } catch (Exception ignored) {}
        String last = uri.getLastPathSegment();
        return last != null ? last : "image";
    }

    // ── History adapter ────────────────────────────────────────────────────

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        interface OnOpen   { void onOpen(File file); }
        interface OnDelete { void onDelete(int position); }

        private final List<File>       items;
        private final OnOpen           onOpen;
        private final OnDelete         onDelete;
        private final SimpleDateFormat sdf =
                new SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault());

        HistoryAdapter(List<File> items, OnOpen onOpen, OnDelete onDelete) {
            this.items    = items;
            this.onOpen   = onOpen;
            this.onDelete = onDelete;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_pdf, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            File file = items.get(position);
            h.tvFileName.setText(file.getName().replace(".pdf", ""));
            h.tvDate.setText(sdf.format(new Date(file.lastModified())));
            h.itemView.setOnClickListener(v -> onOpen.onOpen(file));
            h.btnRemove.setOnClickListener(v -> onDelete.onDelete(h.getAdapterPosition()));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView    tvFileName, tvDate;
            final ImageButton btnRemove;
            VH(View v) {
                super(v);
                tvFileName = v.findViewById(R.id.tvHistoryName);
                tvDate     = v.findViewById(R.id.tvHistoryDate);
                btnRemove  = v.findViewById(R.id.btnHistoryDelete);
            }
        }
    }
}
