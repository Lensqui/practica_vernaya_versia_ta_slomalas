package com.example.myapplication;

import java.util.Date;

public class Notification {
    private String id;
    private String profileId;
    private String message;
    private boolean isRead;
    private Date createdAt;

    public Notification(String id, String profileId, String message, boolean isRead, Date createdAt) {
        this.id = id;
        this.profileId = profileId;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
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

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}