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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

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
        // Use getName() if available, otherwise title
        String displayName = (resume.getName() != null && !resume.getName().isEmpty())
                ? resume.getName() : resume.getTitle();

        holder.nameText.setText(displayName);
        holder.emailText.setText(resume.getEmail());
        holder.dateText.setText(resume.getDate());

        holder.editBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, ResumeEditorActivity.class);
            intent.putExtra("RESUME_ID", resume.getId());
            context.startActivity(intent);
        });

        holder.menuMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.menuMore);
            popup.getMenu().add(0, 0, 0, "Duplicate");
            popup.getMenu().add(0, 1, 1, "Delete");

            popup.setOnMenuItemClickListener(item -> {
                int currentPos = holder.getAdapterPosition();
                if (item.getItemId() == 1) { // DELETE
                    new Thread(() -> {
                        db.resumeDao().delete(resume);
                        ((Activity) context).runOnUiThread(() -> {
                            resumes.remove(currentPos);
                            notifyItemRemoved(currentPos);
                        });
                    }).start();
                } else if (item.getItemId() == 0) { // DUPLICATE
                    new Thread(() -> {
                        // Create a copy of the current resume
                        Resume duplicate = new Resume();
                        duplicate.setTitle(resume.getTitle() + " (Copy)");
                        duplicate.setName(resume.getName());
                        duplicate.setEmail(resume.getEmail());
                        duplicate.setDate("Copied: " + java.text.DateFormat.getDateInstance().format(new java.util.Date()));

                        db.resumeDao().insert(duplicate);

                        // Refresh the full list from DB to show the new item
                        List<Resume> updatedList = db.resumeDao().getAllResumes();

                        ((Activity) context).runOnUiThread(() -> {
                            resumes.clear();
                            resumes.addAll(updatedList);
                            notifyDataSetChanged();
                            Toast.makeText(context, "Resume Duplicated", Toast.LENGTH_SHORT).show();
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