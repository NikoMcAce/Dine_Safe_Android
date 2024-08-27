package com.example.dine_safe_android;
public class OrderItem {
    private String name;
    private int quantity;
    private String price;

    public OrderItem(String name, int quantity, String price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getPrice() {
        return price;
    }
}