package com.example.myapplication;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Order {
    String id, name, imageUrl;
    double totalPrice, pricePerItem;
    int quantity;
    Date createdAt;

    public Order(String id, String name, double totalPrice, double pricePerItem, int quantity, String imageUrl, Date createdAt) {
        this.id = id;
        this.name = name;
        this.totalPrice = totalPrice;
        this.pricePerItem = pricePerItem;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return "Неизвестно";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("ru"));
        return sdf.format(createdAt);
    }

    public String getTimeCategory() {
        if (createdAt == null) {
            return "Неизвестно";
        }
        Calendar now = Calendar.getInstance();
        Calendar orderTime = Calendar.getInstance();
        orderTime.setTime(createdAt);
        long diff = now.getTimeInMillis() - orderTime.getTimeInMillis();

        if (diff < 24 * 60 * 60 * 1000L) {
            return "Недавно";
        } else if (diff < 2 * 24 * 60 * 60 * 1000L) {
            return "Вчера";
        } else if (diff < 30 * 24 * 60 * 60 * 1000L) {
            return "Месяц назад";
        } else {
            return "Год назад";
        }
    }
}