package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatHistoryActivity extends BaseActivity {

    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_NEW_CHAT = "new_chat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnNewChat = findViewById(R.id.btn_new_chat);
        RecyclerView rv = findViewById(R.id.rv_sessions);
        TextView tvEmpty = findViewById(R.id.tv_empty);

        btnBack.setOnClickListener(v -> finish());
        btnNewChat.setOnClickListener(v -> openNewChat());

        ChatHistoryManager mgr = ChatHistoryManager.get(this);
        List<ChatSession> sessions = mgr.loadAllMeta();

        updateEmptyState(sessions, rv, tvEmpty);

        // Use array wrapper so the lambda can reference the adapter
        ChatSessionAdapter[] ref = new ChatSessionAdapter[1];
        ref[0] = new ChatSessionAdapter(sessions, new ChatSessionAdapter.Listener() {
            @Override
            public void onOpen(ChatSession session) {
                Intent intent = new Intent(ChatHistoryActivity.this, AiAssistantActivity.class);
                intent.putExtra(EXTRA_SESSION_ID, session.id);
                startActivity(intent);
                finish();
            }

            @Override
            public void onDelete(ChatSession session, int position) {
                mgr.deleteSession(session.id);
                sessions.remove(position);
                ref[0].notifyItemRemoved(position);
                updateEmptyState(sessions, rv, tvEmpty);
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(ref[0]);
    }

    private void updateEmptyState(List<ChatSession> sessions, RecyclerView rv, TextView tvEmpty) {
        boolean empty = sessions.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openNewChat() {
        Intent intent = new Intent(this, AiAssistantActivity.class);
        intent.putExtra(EXTRA_NEW_CHAT, true);
        startActivity(intent);
        finish();
    }
}
