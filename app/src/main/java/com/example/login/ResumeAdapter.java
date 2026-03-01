package com.example.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResumeAdapter extends RecyclerView.Adapter<ResumeAdapter.ViewHolder> {

    private final List<Resume> resumes;
    private final AppDatabase db;

    public ResumeAdapter(List<Resume> resumes, AppDatabase db) {
        this.resumes = resumes;
        this.db = db;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resume, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Resume resume = resumes.get(position);
        Context context = holder.itemView.getContext();

        // Bind data to XML views
        holder.nameText.setText(resume.getTitle());
        holder.emailText.setText(resume.getEmail());
        holder.dateText.setText(resume.getDate());

        // Edit button goes to the ProfileActivity cards
        holder.editBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("RESUME_ID", resume.getId());
            context.startActivity(intent);
        });

        // Menu More (Delete/Duplicate)
        holder.menuMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.menuMore);
            popup.getMenu().add(0, 0, 0, "Duplicate");
            popup.getMenu().add(0, 1, 1, "Delete");

            popup.setOnMenuItemClickListener(item -> {
                int currentPos = holder.getAdapterPosition();
                if (item.getItemId() == 1) {
                    new Thread(() -> {
                        db.resumeDao().delete(resume);
                        ((Activity) context).runOnUiThread(() -> {
                            resumes.remove(currentPos);
                            notifyItemRemoved(currentPos);
                        });
                    }).start();
                } else if (item.getItemId() == 0) {
                    new Thread(() -> {
                        String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
                        Resume copy = new Resume(resume.getTitle() + " Copy", resume.getEmail(), date);
                        db.resumeDao().insert(copy);
                        ((Activity) context).runOnUiThread(() -> {
                            if (context instanceof MyResumesActivity) ((MyResumesActivity) context).loadResumes();
                        });
                    }).start();
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() { return resumes.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, emailText, dateText;
        ImageButton menuMore;
        MaterialButton editBtn, viewBtn;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            emailText = itemView.findViewById(R.id.emailText);
            dateText = itemView.findViewById(R.id.dateText);
            menuMore = itemView.findViewById(R.id.menuMore);
            editBtn = itemView.findViewById(R.id.editBtn);
            viewBtn = itemView.findViewById(R.id.viewBtn);
        }
    }
}