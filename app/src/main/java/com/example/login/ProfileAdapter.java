package com.example.login;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {

    private List<Profile> list;
    private OnProfileClickListener listener;

    // Interface to handle button clicks in your Activity
    public interface OnProfileClickListener {
        void onEditClick(Profile profile);
        void onViewClick(Profile profile);
    }

    public ProfileAdapter(List<Profile> list, OnProfileClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resume, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int i) {
        Profile p = list.get(i);

        // Setting the data from your Profile model to the UI
        h.name.setText(p.getName());
        h.email.setText(p.getEmail());
        h.date.setText(p.getDate());

        h.edit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(p);
        });

        h.view.setOnClickListener(v -> {
            if (listener != null) listener.onViewClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, email, date;
        Button edit, view;

        public ViewHolder(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.nameText);
            email = v.findViewById(R.id.emailText);
            date = v.findViewById(R.id.dateText);
            edit = v.findViewById(R.id.editBtn);
            view = v.findViewById(R.id.viewBtn);
        }
    }
}