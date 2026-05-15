package com.example.login;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EducationListAdapter extends RecyclerView.Adapter<EducationListAdapter.ViewHolder> {

    public interface Listener {
        void onEntryClick(int position);
        void onEntryDelete(int position);
    }

    private final List<EducationEntry> entries;
    private final Listener listener;

    public EducationListAdapter(List<EducationEntry> entries, Listener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_education_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EducationEntry e = entries.get(position);
        holder.school.setText(displayOrPlaceholder(e.schoolName, "Untitled"));
        holder.degree.setText(displayOrPlaceholder(e.degree, ""));
        holder.degree.setVisibility(isEmpty(e.degree) ? View.GONE : View.VISIBLE);
        holder.date.setText(displayOrPlaceholder(e.schoolDate, ""));
        holder.date.setVisibility(isEmpty(e.schoolDate) ? View.GONE : View.VISIBLE);

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
        final TextView school;
        final TextView degree;
        final TextView date;
        final ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            school = itemView.findViewById(R.id.cardSchool);
            degree = itemView.findViewById(R.id.cardDegree);
            date = itemView.findViewById(R.id.cardDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteEntry);
        }
    }
}
