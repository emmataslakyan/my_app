package com.example.login;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import java.util.List;

public class MergedPdfPreviewActivity extends BaseActivity {

    private RecyclerView rvPages;
    private ProgressBar  loading;
    private String       localPath;
    private String       fileName;

    private final ActivityResultLauncher<Intent> saveFileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    writeToUri(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merged_pdf_preview);
        setupCommonToolbar();

        localPath = getIntent().getStringExtra("LOCAL_PATH");
        fileName  = getIntent().getStringExtra("FILE_NAME");

        rvPages = findViewById(R.id.rvPages);
        loading = findViewById(R.id.loading);
        rvPages.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnDownload).setOnClickListener(v -> openSavePicker());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareFile());

        renderPages();
    }

    private void renderPages() {
        loading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<Bitmap> pages = new ArrayList<>();
            try {
                File file = new File(localPath);
                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer renderer = new PdfRenderer(pfd);

                int screenW = getResources().getDisplayMetrics().widthPixels - 48;

                for (int i = 0; i < renderer.getPageCount(); i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    int bmpH = (int) ((float) page.getHeight() / page.getWidth() * screenW);
                    Bitmap bmp = Bitmap.createBitmap(screenW, bmpH, Bitmap.Config.ARGB_8888);
                    bmp.eraseColor(Color.WHITE);
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();
                    pages.add(bmp);
                }
                renderer.close();
                pfd.close();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Render error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
            runOnUiThread(() -> {
                loading.setVisibility(View.GONE);
                rvPages.setAdapter(new PageAdapter(pages));
            });
        }).start();
    }

    private void openSavePicker() {
        String title = (fileName != null && !fileName.isEmpty())
                ? fileName + ".pdf"
                : "merged_" + System.currentTimeMillis() + ".pdf";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, title);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        saveFileLauncher.launch(intent);
    }

    private void writeToUri(Uri destinationUri) {
        new Thread(() -> {
            try (InputStream in  = new FileInputStream(new File(localPath));
                 OutputStream out = getContentResolver().openOutputStream(destinationUri)) {
                if (out == null) throw new IOException("Cannot open output stream");
                copyStream(in, out);
                runOnUiThread(() -> Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void shareFile() {
        File file = new File(localPath);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share PDF"));
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
    }

    // ── Adapter ────────────────────────────────────────────────────────────

    private static class PageAdapter extends RecyclerView.Adapter<PageAdapter.VH> {

        private final List<Bitmap> pages;

        PageAdapter(List<Bitmap> pages) { this.pages = pages; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pdf_page, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.ivPage.setImageBitmap(pages.get(position));
        }

        @Override
        public int getItemCount() { return pages.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView ivPage;
            VH(View v) { super(v); ivPage = v.findViewById(R.id.ivPage); }
        }
    }
}
