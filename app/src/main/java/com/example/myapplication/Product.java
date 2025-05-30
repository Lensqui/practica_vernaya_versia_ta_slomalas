package com.example.myapplication;

public class Product {
    private String id;
    private String name;
    private String price;
    private String category;
    private String imageUrl;
    private String imageUrlForDetails;
    private String imageUrlForDetails2;
    private String imageUrlForDetails3;
    private String imageUrlForDetails4;
    private String imageUrlForDetails5;
    private String textForDetails;

    public Product(String id, String name, String price, String category, String imageUrl,
                   String imageUrlForDetails, String imageUrlForDetails2, String imageUrlForDetails3,
                   String imageUrlForDetails4, String imageUrlForDetails5, String textForDetails) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.imageUrlForDetails = imageUrlForDetails;
        this.imageUrlForDetails2 = imageUrlForDetails2;
        this.imageUrlForDetails3 = imageUrlForDetails3;
        this.imageUrlForDetails4 = imageUrlForDetails4;
        this.imageUrlForDetails5 = imageUrlForDetails5;
        this.textForDetails = textForDetails;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getImageUrlForDetails() {
        return imageUrlForDetails;
    }

    public String getImageUrlForDetails2() {
        return imageUrlForDetails2;
    }

    public String getImageUrlForDetails3() {
        return imageUrlForDetails3;
    }

    public String getImageUrlForDetails4() {
        return imageUrlForDetails4;
    }

    public String getImageUrlForDetails5() {
        return imageUrlForDetails5;
    }

    public String getTextForDetails() {
        return textForDetails;
    }
}