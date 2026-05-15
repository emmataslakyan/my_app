package com.example.login;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.VH> {

    public interface OnTemplateClick {
        void onTemplateClick(ResumeTemplate template);
    }

    private final List<ResumeTemplate> items = new ArrayList<>();
    private final OnTemplateClick listener;

    public TemplateAdapter(OnTemplateClick listener) {
        this.listener = listener;
    }

    public void submit(List<ResumeTemplate> templates) {
        items.clear();
        if (templates != null) items.addAll(templates);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_template_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ResumeTemplate t = items.get(position);
        holder.name.setText(t.getName());
        if (t.getThumbnailUrl() != null && !t.getThumbnailUrl().isEmpty()) {
            Glide.with(holder.thumb)
                    .load(t.getThumbnailUrl())
                    .placeholder(R.drawable.tamplete)
                    .error(R.drawable.tamplete)
                    .into(holder.thumb);
        } else {
            holder.thumb.setImageResource(R.drawable.tamplete);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTemplateClick(t);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView name;
        VH(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.templateThumb);
            name = itemView.findViewById(R.id.templateName);
        }
    }
}
