package com.example.fooddeliverytracking.models;

/** A menu item and its selected quantity inside an order. */
public class OrderItem {

    private String name;
    private int quantity;
    private double price;

    public OrderItem() {
        // Required by Firebase Realtime Database.
    }

    public OrderItem(String name, int quantity, double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getSubtotal() {
        return quantity * price;
    }
}
