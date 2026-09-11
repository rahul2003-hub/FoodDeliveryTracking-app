package com.example.fooddeliverytracking.models;

import com.example.fooddeliverytracking.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/** Complete order record stored under the Firebase orders node. */
public class Order {

    private String orderId;
    private String customerId;
    private String customerName;
    private String customerAddress;
    private double customerLat;
    private double customerLng;
    private String restaurantName;
    private String restaurantAddress;
    private double restaurantLat;
    private double restaurantLng;
    private String driverId;
    private String driverName;
    private String status;
    private List<OrderItem> items;
    private double totalAmount;
    private long createdAt;
    private DriverLocation driverLocation;
    private Long estimatedArrivalAt;

    public Long getEstimatedArrivalAt() { return estimatedArrivalAt; }

    public void setEstimatedArrivalAt(Long estimatedArrivalAt) { this.estimatedArrivalAt = estimatedArrivalAt; }

    public Order() {
        // Required by Firebase Realtime Database.
    }

    public Order(String orderId, String customerId, String customerName, String customerAddress,
                 List<OrderItem> items) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.items = items == null ? new ArrayList<>() : items;
        this.status = Constants.STATUS_PLACED;
        calculateTotal();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public double getCustomerLat() {
        return customerLat;
    }

    public void setCustomerLat(double customerLat) {
        this.customerLat = customerLat;
    }

    public double getCustomerLng() {
        return customerLng;
    }

    public void setCustomerLng(double customerLng) {
        this.customerLng = customerLng;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getRestaurantAddress() {
        return restaurantAddress;
    }

    public void setRestaurantAddress(String restaurantAddress) {
        this.restaurantAddress = restaurantAddress;
    }

    public double getRestaurantLat() {
        return restaurantLat;
    }

    public void setRestaurantLat(double restaurantLat) {
        this.restaurantLat = restaurantLat;
    }

    public double getRestaurantLng() {
        return restaurantLng;
    }

    public void setRestaurantLng(double restaurantLng) {
        this.restaurantLng = restaurantLng;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public DriverLocation getDriverLocation() {
        return driverLocation;
    }

    public void setDriverLocation(DriverLocation driverLocation) {
        this.driverLocation = driverLocation;
    }

    /** Recalculates and returns the sum of all item subtotals. */
    public double calculateTotal() {
        double total = 0.0;
        if (items != null) {
            for (OrderItem item : items) {
                if (item != null) {
                    total += item.getSubtotal();
                }
            }
        }
        totalAmount = total;
        return total;
    }
}
