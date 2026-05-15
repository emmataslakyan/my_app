package com.example.login;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExperienceListAdapter extends RecyclerView.Adapter<ExperienceListAdapter.ViewHolder> {

    public interface Listener {
        void onEntryClick(int position);
        void onEntryDelete(int position);
    }

    private final List<ExperienceEntry> entries;
    private final Listener listener;

    public ExperienceListAdapter(List<ExperienceEntry> entries, Listener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_experience_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExperienceEntry e = entries.get(position);
        holder.org.setText(displayOrPlaceholder(e.expOrgName, "Untitled"));
        holder.position.setText(displayOrPlaceholder(e.expPosition, ""));
        holder.position.setVisibility(isEmpty(e.expPosition) ? View.GONE : View.VISIBLE);
        holder.date.setText(displayOrPlaceholder(e.expDate, ""));
        holder.date.setVisibility(isEmpty(e.expDate) ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onEntryClick(pos);
        });
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onEntryDelete(pos);
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private static String displayOrPlaceholder(String s, String fallback) {
        return isEmpty(s) ? fallback : s;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView org;
        final TextView position;
        final TextView date;
        final ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            org = itemView.findViewById(R.id.cardOrg);
            position = itemView.findViewById(R.id.cardPosition);
            date = itemView.findViewById(R.id.cardDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteEntry);
        }
    }
}
