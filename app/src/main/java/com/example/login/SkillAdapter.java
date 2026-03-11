package com.example.login;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;

public class SkillAdapter extends RecyclerView.Adapter<SkillAdapter.SkillViewHolder> {

    private final List<String> skillsList; // Stores format "SkillName:Level"
    private final String[] levels = {"Beginner", "Intermediate", "Advanced", "Expert"};

    public SkillAdapter(List<String> skillsList) {
        this.skillsList = skillsList;
    }

    @NonNull
    @Override
    public SkillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_skill, parent, false);
        return new SkillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SkillViewHolder holder, int position) {
        String current = skillsList.get(position);
        String[] parts = current.split(":", -1);

        String name = parts.length > 0 ? parts[0] : "";
        String level = parts.length > 1 ? parts[1] : "Beginner";

        // Set the Skill Name
        holder.editName.setText(name);

        // Setup the Dropdown Adapter
        Context context = holder.itemView.getContext();
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, levels);
        holder.autoCompleteLevel.setAdapter(levelAdapter);
        holder.autoCompleteLevel.setText(level, false);

        // Update list when text changes
        holder.editName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                skillsList.set(holder.getAdapterPosition(), s.toString() + ":" + holder.autoCompleteLevel.getText().toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Update list when level is selected
        holder.autoCompleteLevel.setOnItemClickListener((parent, view, pos, id) -> {
            String selectedLevel = levels[pos];
            skillsList.set(holder.getAdapterPosition(), holder.editName.getText().toString() + ":" + selectedLevel);
        });

        // Delete Row
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                skillsList.remove(pos);
                notifyItemRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return skillsList.size();
    }

    public void addSkill() {
        skillsList.add(":Beginner");
        notifyItemInserted(skillsList.size() - 1);
    }

    public List<String> getSkillsList() {
        return skillsList;
    }

    public static class SkillViewHolder extends RecyclerView.ViewHolder {
        TextInputEditText editName;
        AutoCompleteTextView autoCompleteLevel;
        ImageButton btnDelete;

        public SkillViewHolder(@NonNull View itemView) {
            super(itemView);
            editName = itemView.findViewById(R.id.editSkillName);
            autoCompleteLevel = itemView.findViewById(R.id.autoCompleteLevel);
            btnDelete = itemView.findViewById(R.id.btnDeleteSkill);
        }
    }
}
