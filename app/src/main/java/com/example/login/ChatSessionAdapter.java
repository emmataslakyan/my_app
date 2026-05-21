package com.example.login;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.VH> {

    public interface Listener {
        void onOpen(ChatSession session);
        void onDelete(ChatSession session, int position);
    }

    private final List<ChatSession> sessions;
    private final Listener listener;

    public ChatSessionAdapter(List<ChatSession> sessions, Listener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_session, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatSession s = sessions.get(position);
        h.tvTitle.setText(s.title);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
        h.tvDate.setText(sdf.format(new Date(s.updatedAt)));
        h.itemView.setOnClickListener(v -> listener.onOpen(s));
        h.btnDelete.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onDelete(s, pos);
        });
    }

    @Override
    public int getItemCount() { return sessions.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate;
        ImageButton btnDelete;

        VH(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_session_title);
            tvDate = v.findViewById(R.id.tv_session_date);
            btnDelete = v.findViewById(R.id.btn_delete_session);
        }
    }
}
