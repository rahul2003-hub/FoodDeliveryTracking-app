package com.example.fooddeliverytracking.models;

/** A food item displayed in the customer menu. */
public class MenuItem {

    private String id;
    private String name;
    private String description;
    private double price;
    private int imageResId;

    public MenuItem() {
        // Required by Firebase Realtime Database.
    }

    public MenuItem(String id, String name, String description, double price, int imageResId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageResId = imageResId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }
}
