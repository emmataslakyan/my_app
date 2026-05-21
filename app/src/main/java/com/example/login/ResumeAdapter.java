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

    public interface Listener {
        void onResumeChanged();
    }

    private final List<Resume> resumes;
    private final ResumeRepository repo;
    private final Listener listener;

    public ResumeAdapter(List<Resume> resumes, ResumeRepository repo, Listener listener) {
        this.resumes = resumes;
        this.repo = repo;
        this.listener = listener;
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
        holder.viewBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, TemplateSelectionActivity.class);
            intent.putExtra("RESUME_ID", resume.getId());
            context.startActivity(intent);
        });
        holder.menuMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.menuMore);
            popup.getMenu().add(0, 0, 0, "Duplicate");
            popup.getMenu().add(0, 1, 1, "Delete");

            popup.setOnMenuItemClickListener(item -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return true;
                if (item.getItemId() == 1) {
                    repo.delete(resume.getId(),
                            () -> ((Activity) context).runOnUiThread(() -> {
                                int idx = resumes.indexOf(resume);
                                if (idx >= 0) {
                                    resumes.remove(idx);
                                    notifyItemRemoved(idx);
                                }
                            }),
                            err -> ((Activity) context).runOnUiThread(() ->
                                    Toast.makeText(context, "Delete failed: " + err, Toast.LENGTH_SHORT).show()));
                } else if (item.getItemId() == 0) {
                    Resume duplicate = new Resume();
                    duplicate.setTitle((resume.getTitle() == null ? "" : resume.getTitle()) + " (Copy)");
                    duplicate.setName(resume.getName());
                    duplicate.setEmail(resume.getEmail());
                    duplicate.setPhone(resume.getPhone());
                    duplicate.setAddress(resume.getAddress());
                    duplicate.setPhotoPath(resume.getPhotoPath());
                    duplicate.setTemplateId(resume.getTemplateId());
                    duplicate.setEducationJson(resume.getEducationJson());
                    duplicate.setExperienceJson(resume.getExperienceJson());
                    duplicate.setSkills(resume.getSkills());
                    duplicate.setLanguages(resume.getLanguages());
                    duplicate.setDate("Copied: " +
                            java.text.DateFormat.getDateInstance().format(new java.util.Date()));

                    repo.create(duplicate,
                            id -> ((Activity) context).runOnUiThread(() -> {
                                Toast.makeText(context, "Resume Duplicated", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onResumeChanged();
                            }),
                            err -> ((Activity) context).runOnUiThread(() ->
                                    Toast.makeText(context, "Duplicate failed: " + err, Toast.LENGTH_SHORT).show()));
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
