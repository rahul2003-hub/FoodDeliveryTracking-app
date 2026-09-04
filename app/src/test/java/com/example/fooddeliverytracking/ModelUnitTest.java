package com.example.fooddeliverytracking;

import static org.junit.Assert.assertEquals;

import com.example.fooddeliverytracking.models.Order;
import com.example.fooddeliverytracking.models.OrderItem;
import com.example.fooddeliverytracking.utils.Constants;

import org.junit.Test;

import java.util.Arrays;

public class ModelUnitTest {

    @Test
    public void orderCalculatesTotalAndAdvancesStatus() {
        OrderItem burger = new OrderItem("Burger", 2, 120.0);
        OrderItem drink = new OrderItem("Drink", 1, 40.0);
        Order order = new Order("order-101", "customer-1", "Asha", "12 College Road",
                Arrays.asList(burger, drink));

        assertEquals("order-101", order.getOrderId());
        assertEquals(Constants.STATUS_PLACED, order.getStatus());
        assertEquals(280.0, order.calculateTotal(), 0.001);

        order.setStatus(Constants.STATUS_ACCEPTED);
        assertEquals(Constants.STATUS_ACCEPTED, order.getStatus());
        order.setStatus(Constants.STATUS_PICKED_UP);
        assertEquals(Constants.STATUS_PICKED_UP, order.getStatus());
        order.setStatus(Constants.STATUS_OUT_FOR_DELIVERY);
        assertEquals(Constants.STATUS_OUT_FOR_DELIVERY, order.getStatus());
        order.setStatus(Constants.STATUS_DELIVERED);
        assertEquals(Constants.STATUS_DELIVERED, order.getStatus());
    }
}
