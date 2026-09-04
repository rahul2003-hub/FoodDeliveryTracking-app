package com.example.fooddeliverytracking.utils;

/** Shared values used by the app and stored in Firebase. */
public final class Constants {

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_DRIVER = "DRIVER";

    public static final String STATUS_PLACED = "PLACED";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_PICKED_UP = "PICKED_UP";
    public static final String STATUS_OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String STATUS_DELIVERED = "DELIVERED";

    public static final String NODE_USERS = "users";
    public static final String NODE_ORDERS = "orders";

    private Constants() {
        // Utility class.
    }
}
