package com.example.myapplication;

public class Promotion {
    private String id;
    private String imageUrl;

    public Promotion(String id, String imageUrl) {
        this.id = id;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
