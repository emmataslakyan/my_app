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

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LangViewHolder> {

    private final List<String> langList; // "Language:Proficiency"
    private final String[] levels = {"Native", "Fluent", "Advanced", "Intermediate", "Basic"};

    public LanguageAdapter(List<String> langList) {
        this.langList = langList;
    }

    @NonNull
    @Override
    public LangViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);
        return new LangViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LangViewHolder holder, int position) {
        String current = langList.get(position);
        String[] parts = current.split(":", -1);
        String name  = parts.length > 0 ? parts[0] : "";
        String level = parts.length > 1 ? parts[1] : "Fluent";

        holder.editName.setText(name);

        Context context = holder.itemView.getContext();
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_dropdown_item_1line, levels);
        holder.autoCompleteProf.setAdapter(levelAdapter);
        holder.autoCompleteProf.setText(level, false);

        holder.editName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION)
                    langList.set(pos, s.toString() + ":" + holder.autoCompleteProf.getText().toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        holder.autoCompleteProf.setOnItemClickListener((parent, view, pos, id) -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION)
                langList.set(adapterPos, holder.editName.getText().toString() + ":" + levels[pos]);
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                langList.remove(pos);
                notifyItemRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() { return langList.size(); }

    public void addLanguage() {
        langList.add(":Fluent");
        notifyItemInserted(langList.size() - 1);
    }

    public List<String> getLangList() { return langList; }

    public static class LangViewHolder extends RecyclerView.ViewHolder {
        final TextInputEditText editName;
        final AutoCompleteTextView autoCompleteProf;
        final ImageButton btnDelete;

        public LangViewHolder(@NonNull View itemView) {
            super(itemView);
            editName         = itemView.findViewById(R.id.editLanguageName);
            autoCompleteProf = itemView.findViewById(R.id.autoCompleteProf);
            btnDelete        = itemView.findViewById(R.id.btnDeleteLanguage);
        }
    }
}
