package com.example.login;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProgramAdapter extends RecyclerView.Adapter<ProgramAdapter.ViewHolder> {

    private final List<Program> programs;

    public ProgramAdapter(List<Program> programs) {
        this.programs = programs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_program_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Program p = programs.get(position);
        holder.title.setText(p.title);

        if (p.meta == null || p.meta.isEmpty()) {
            holder.meta.setVisibility(View.GONE);
        } else {
            holder.meta.setVisibility(View.VISIBLE);
            holder.meta.setText(p.meta);
        }

        if (p.imageUrl != null && !p.imageUrl.isEmpty()) {
            Glide.with(holder.image.getContext())
                    .load(p.imageUrl)
                    .centerCrop()
                    .into(holder.image);
        } else {
            holder.image.setImageDrawable(null);
        }

        holder.itemView.setOnClickListener(v -> openInBrowser(v.getContext(), p.programUrl));
    }

    @Override
    public int getItemCount() {
        return programs.size();
    }

    private static void openInBrowser(Context ctx, String url) {
        if (url == null || url.isEmpty()) return;
        String safe = url;
        if (!safe.startsWith("http://") && !safe.startsWith("https://")) {
            safe = "https://" + safe;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(safe));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;
        final TextView meta;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.programImage);
            title = itemView.findViewById(R.id.programTitle);
            meta = itemView.findViewById(R.id.programMeta);
        }
    }
}
