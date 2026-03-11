package com.example.login;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BulletAdapter extends RecyclerView.Adapter<BulletAdapter.ViewHolder> {
    private final List<String> bullets;

    public BulletAdapter(List<String> bullets) {
        this.bullets = bullets;
    }

    public void addBullet() {
        bullets.add("");
        notifyItemInserted(bullets.size() - 1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bullet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.editBullet.setText(bullets.get(position));

        // Remove existing listener to avoid recursion issues
        if (holder.textWatcher != null) {
            holder.editBullet.removeTextChangedListener(holder.textWatcher);
        }

        holder.textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    bullets.set(pos, s.toString());
                }
            }
        };
        holder.editBullet.addTextChangedListener(holder.textWatcher);

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                bullets.remove(pos);
                notifyItemRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bullets.size();
    }

    public List<String> getBullets() {
        return bullets;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        EditText editBullet;
        ImageButton btnDelete;
        TextWatcher textWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            editBullet = itemView.findViewById(R.id.editBullet);
            btnDelete = itemView.findViewById(R.id.btnDeleteBullet);
        }
    }
}