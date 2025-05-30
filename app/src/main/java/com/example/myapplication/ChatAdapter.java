package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;

    private final List<ChatMessage> messages;
    private final String userAvatarUrl;
    private final Context context;

    public ChatAdapter(List<ChatMessage> messages, String userAvatarUrl, Context context) {
        this.messages = messages;
        this.userAvatarUrl = userAvatarUrl;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return message.getSender().equals("user") ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_bot, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (holder instanceof UserMessageViewHolder) {
            UserMessageViewHolder userHolder = (UserMessageViewHolder) holder;
            userHolder.messageText.setText(message.getMessage());
            userHolder.messageName.setText("Вы");
            userHolder.messageTime.setText(sdf.format(message.getCreatedAt()));
            userHolder.messageStatus.setImageResource(message.isRead() ? R.drawable.ic_double_check : R.drawable.ic_single_check);
            if (userAvatarUrl != null && !userAvatarUrl.isEmpty()) {
                Glide.with(context)
                        .load(userAvatarUrl)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(userHolder.messageAvatar);
            } else {
                userHolder.messageAvatar.setImageResource(R.drawable.default_avatar);
            }
        } else {
            BotMessageViewHolder botHolder = (BotMessageViewHolder) holder;
            botHolder.messageText.setText(message.getMessage());
            botHolder.messageName.setText("Поддержка");
            botHolder.messageTime.setText(sdf.format(message.getCreatedAt()));
            botHolder.messageAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateMessageReadStatus(int position) {
        messages.get(position).setRead(true);
        notifyItemChanged(position);
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageName, messageText, messageTime;
        ImageView messageAvatar, messageStatus;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageName = itemView.findViewById(R.id.message_name);
            messageText = itemView.findViewById(R.id.message_text);
            messageTime = itemView.findViewById(R.id.message_time);
            messageAvatar = itemView.findViewById(R.id.message_avatar);
            messageStatus = itemView.findViewById(R.id.message_status);
        }
    }

    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageName, messageText, messageTime;
        ImageView messageAvatar;

        BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageName = itemView.findViewById(R.id.message_name);
            messageText = itemView.findViewById(R.id.message_text);
            messageTime = itemView.findViewById(R.id.message_time);
            messageAvatar = itemView.findViewById(R.id.message_avatar);
        }
    }
}
