package com.example.myapplication;

import java.util.Date;

public class ChatMessage {
    private String id;
    private String profileId;
    private String message;
    private String sender;
    private Date createdAt;
    private boolean isRead;

    public ChatMessage(String id, String profileId, String message, String sender, Date createdAt, boolean isRead) {
        this.id = id;
        this.profileId = profileId;
        this.message = message;
        this.sender = sender;
        this.createdAt = createdAt;
        this.isRead = isRead;
    }

    public String getId() {
        return id;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
