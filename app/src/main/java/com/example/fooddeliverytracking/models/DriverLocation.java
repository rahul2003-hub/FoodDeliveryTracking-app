package com.example.fooddeliverytracking.models;

/** Latest GPS location broadcast by the driver for an order. */
public class DriverLocation {

    private double latitude;
    private double longitude;
    private long updatedAt;

    public DriverLocation() {
        // Required by Firebase Realtime Database.
    }

    public DriverLocation(double latitude, double longitude, long updatedAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = updatedAt;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
