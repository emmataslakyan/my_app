package com.example.login;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;

public class CompressPdfActivity extends BaseActivity {

    private enum Level { LIGHT, MEDIUM, HEAVY }

    // Render scale and JPEG quality per level
    private static final float[] SCALES    = {2.0f, 1.5f, 1.0f};
    private static final int[]   QUALITIES = {85,   65,   45};

    private static final File[] EMPTY_FILES = new File[0];

    private Uri    selectedUri;
    private long   sourceBytes;
    private Level  selectedLevel = Level.MEDIUM;

    private final List<File> historyFiles  = new ArrayList<>();
    private HistoryAdapter   historyAdapter;

    private View             emptyState;
    private View             historySection;
    private View             fileInfoCard;
    private View             compressionSection;
    private TextView         tvFileName;
    private TextView         tvFileSize;
    private MaterialCardView cardLight, cardMedium, cardHeavy;
    private MaterialButton   btnCompress;

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
        setContentView(R.layout.activity_compress_pdf);
        setupCommonToolbar();

        emptyState         = findViewById(R.id.emptyState);
        historySection     = findViewById(R.id.historySection);
        fileInfoCard       = findViewById(R.id.fileInfoCard);
        compressionSection = findViewById(R.id.compressionSection);
        tvFileName         = findViewById(R.id.tvFileName);
        tvFileSize         = findViewById(R.id.tvFileSize);
        cardLight          = findViewById(R.id.cardLight);
        cardMedium         = findViewById(R.id.cardMedium);
        cardHeavy          = findViewById(R.id.cardHeavy);
        btnCompress        = findViewById(R.id.btnCompress);

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(historyFiles, this::onOpenHistory, this::onDeleteHistory);
        rvHistory.setAdapter(historyAdapter);

        cardLight.setOnClickListener(v  -> selectLevel(Level.LIGHT));
        cardMedium.setOnClickListener(v -> selectLevel(Level.MEDIUM));
        cardHeavy.setOnClickListener(v  -> selectLevel(Level.HEAVY));
        selectLevel(Level.MEDIUM);

        findViewById(R.id.btnSelectFile).setOnClickListener(v -> openFilePicker());
        btnCompress.setOnClickListener(v -> startCompress());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    // ── Level selection ────────────────────────────────────────────────────

    private void selectLevel(Level level) {
        selectedLevel = level;
        applyCardState(cardLight,  level == Level.LIGHT);
        applyCardState(cardMedium, level == Level.MEDIUM);
        applyCardState(cardHeavy,  level == Level.HEAVY);
    }

    private void applyCardState(MaterialCardView card, boolean selected) {
        card.setStrokeWidth(selected ? 4 : 2);
        card.setStrokeColor(selected ? 0xFF46287A : 0xFFD0C8E8);
        card.setCardElevation(selected ? 6f : 0f);
    }

    // ── File picker ────────────────────────────────────────────────────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select PDF to compress"));
    }

    private void loadFile(Uri uri) {
        selectedUri = uri;
        sourceBytes = queryFileSize(uri);

        String raw  = uri.getLastPathSegment();
        String name = (raw != null && raw.contains("/"))
                ? raw.substring(raw.lastIndexOf('/') + 1)
                : (raw != null ? raw : "document.pdf");

        tvFileName.setText(name);
        tvFileSize.setText(formatSize(sourceBytes));

        emptyState.setVisibility(View.GONE);
        fileInfoCard.setVisibility(View.VISIBLE);
        compressionSection.setVisibility(View.VISIBLE);
        btnCompress.setEnabled(true);
    }

    private long queryFileSize(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int col = c.getColumnIndex(OpenableColumns.SIZE);
                if (col != -1) return c.getLong(col);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "unknown size";
        if (bytes < 1024 * 1024)
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f);
        return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024f * 1024f));
    }

    // ── History ────────────────────────────────────────────────────────────

    private File historyDir() {
        File dir = new File(getFilesDir(), "compress_history");
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

    // ── Compress flow ──────────────────────────────────────────────────────

    private void startCompress() {
        setButtonState("Compressing…", false);

        File output = new File(historyDir(), "compressed_" + System.currentTimeMillis() + ".pdf");

        new Thread(() -> {
            String error = compressPdf(selectedUri, output);
            if (error != null) {
                runOnUiThread(() -> {
                    setButtonState("Compress", true);
                    Toast.makeText(this, "Failed: " + error, Toast.LENGTH_LONG).show();
                });
                return;
            }

            long outBytes = output.length();
            String summary = formatSize(sourceBytes) + " → " + formatSize(outBytes);
            if (sourceBytes > 0) {
                int pct = (int) (100 - (outBytes * 100f / sourceBytes));
                summary += " (" + Math.max(0, pct) + "% smaller)";
            }
            String finalSummary = summary;

            runOnUiThread(() -> {
                Toast.makeText(this, finalSummary, Toast.LENGTH_LONG).show();
                setButtonState("Uploading…", false);
                uploadToFirebase(output);
            });
        }).start();
    }

    private void uploadToFirebase(File localFile) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid  = user != null ? user.getUid() : "anon";
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("compressed_pdfs/" + uid + "/" + localFile.getName());
        ref.putFile(Uri.fromFile(localFile))
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful())
                        Toast.makeText(this, "Upload failed — file saved locally", Toast.LENGTH_SHORT).show();
                    launchPreview(localFile);
                });
    }

    private void launchPreview(File file) {
        setButtonState("Compress", true);
        Intent intent = new Intent(this, MergedPdfPreviewActivity.class);
        intent.putExtra("LOCAL_PATH", file.getAbsolutePath());
        intent.putExtra("FILE_NAME", file.getName().replace(".pdf", ""));
        startActivity(intent);
    }

    private void setButtonState(String label, boolean enabled) {
        btnCompress.setText(label);
        btnCompress.setEnabled(enabled);
    }

    // ── PDF compression ────────────────────────────────────────────────────
    // Lossless: renders pages at native resolution, applies PNG Up-filter row by
    // row, deflates with zlib BEST_COMPRESSION, and embeds as FlateDecode image
    // XObjects — typically 50-80% smaller for text/document PDFs.
    // Lossy (Medium/Heavy): renders at reduced scale, round-trips through JPEG,
    // and embeds raw JPEG bytes with /DCTDecode — no PdfDocument intermediate,
    // so the output size matches the JPEG quality instead of swelling.

    private String compressPdf(Uri uri, File output) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) return "Cannot open file";
            PdfRenderer renderer = new PdfRenderer(pfd);
            int pageCount = renderer.getPageCount();

            int[] nativeW = new int[pageCount];
            int[] nativeH = new int[pageCount];
            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page p = renderer.openPage(i);
                nativeW[i] = p.getWidth();
                nativeH[i] = p.getHeight();
                p.close();
            }

            if (selectedLevel == Level.LIGHT) {
                List<byte[]> deflated = new ArrayList<>();
                List<int[]>  sizes    = new ArrayList<>();
                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    int w = nativeW[i], h = nativeH[i];
                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    bmp.eraseColor(Color.WHITE);
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                    page.close();
                    deflated.add(deflateRgb(bmp, w, h));
                    bmp.recycle();
                    sizes.add(new int[]{w, h});
                }
                renderer.close(); pfd.close();
                writeFlatePdf(deflated, nativeW, nativeH, sizes, output);
            } else {
                float scale   = SCALES[selectedLevel.ordinal()];
                int   quality = QUALITIES[selectedLevel.ordinal()];
                List<byte[]> jpegs = new ArrayList<>();
                List<int[]>  sizes = new ArrayList<>();
                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    int bmpW = Math.max(1, (int)(nativeW[i] * scale));
                    int bmpH = Math.max(1, (int)(nativeH[i] * scale));
                    Bitmap bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888);
                    bmp.eraseColor(Color.WHITE);
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                    page.close();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                    bmp.recycle();
                    jpegs.add(baos.toByteArray());
                    sizes.add(new int[]{bmpW, bmpH});
                }
                renderer.close(); pfd.close();
                writeJpegPdf(jpegs, nativeW, nativeH, sizes, output);
            }
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    // Extracts raw RGB bytes from bitmap and deflates with zlib BEST_COMPRESSION.
    private byte[] deflateRgb(Bitmap bmp, int w, int h) {
        int[] pixels = new int[w];
        byte[] row   = new byte[w * 3];
        ByteArrayOutputStream rgb = new ByteArrayOutputStream(w * h * 3);
        for (int y = 0; y < h; y++) {
            bmp.getPixels(pixels, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x++) {
                int px = pixels[x];
                row[x*3]   = (byte)((px >> 16) & 0xFF);
                row[x*3+1] = (byte)((px >> 8)  & 0xFF);
                row[x*3+2] = (byte)(px & 0xFF);
            }
            rgb.write(row, 0, row.length);
        }
        byte[] raw = rgb.toByteArray();
        Deflater def = new Deflater(Deflater.BEST_COMPRESSION);
        def.setInput(raw);
        def.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!def.finished()) { int n = def.deflate(buf); out.write(buf, 0, n); }
        def.end();
        return out.toByteArray();
    }

    // Writes a PDF with FlateDecode image XObjects (lossless).
    private void writeFlatePdf(List<byte[]> deflated, int[] nativeW, int[] nativeH,
                                List<int[]> sizes, File output) throws IOException {
        int n = deflated.size();
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
                int pw = nativeW[i], ph = nativeH[i];
                int iw = sizes.get(i)[0], ih = sizes.get(i)[1];
                byte[] data = deflated.get(i);
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
                        + " /Filter /FlateDecode"
                        + " /Length " + data.length + " >>\nstream\n");
                fos.write(data); pos += data.length;
                pos += wr(fos, "\nendstream\nendobj\n");
            }

            pos = writeXref(fos, offsets, n, pos);
        }
    }

    // Writes a PDF with DCTDecode (JPEG) image XObjects (lossy).
    private void writeJpegPdf(List<byte[]> jpegs, int[] nativeW, int[] nativeH,
                               List<int[]> sizes, File output) throws IOException {
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
                int pw = nativeW[i], ph = nativeH[i];
                int iw = sizes.get(i)[0], ih = sizes.get(i)[1];
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

            writeXref(fos, offsets, n, pos);
        }
    }

    private long writeXref(FileOutputStream fos, List<Long> offsets, int n, long pos) throws IOException {
        long xrefOffset = pos;
        int totalObjs = 2 + n * 3;
        StringBuilder xref = new StringBuilder("xref\n0 " + (totalObjs + 1) + "\n");
        xref.append("0000000000 65535 f \n");
        for (long off : offsets) xref.append(String.format(Locale.US, "%010d 00000 n \n", off));
        wr(fos, xref.toString());
        wr(fos, "trailer\n<< /Size " + (totalObjs + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n");
        return xrefOffset;
    }

    private long wr(FileOutputStream fos, String s) throws IOException {
        byte[] b = s.getBytes("ASCII");
        fos.write(b);
        return b.length;
    }

    // ── History adapter ────────────────────────────────────────────────────

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        interface OnOpen   { void onOpen(File file); }
        interface OnDelete { void onDelete(int position); }

        private final List<File>       items;
        private final OnOpen           onOpen;
        private final OnDelete         onDelete;
        private final SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault());

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
