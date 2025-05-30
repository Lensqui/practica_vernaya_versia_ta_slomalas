package com.example.myapplication;

public class OrderItem {
    String id, name, imageUrl, orderId;
    double price;
    int quantity;

    public OrderItem(String id, String name, double price, int quantity, String imageUrl, String orderId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.orderId = orderId;
    }
}