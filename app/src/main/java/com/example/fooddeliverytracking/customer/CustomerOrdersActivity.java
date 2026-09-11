package com.example.fooddeliverytracking.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.adapters.OrderAdapter;
import com.example.fooddeliverytracking.models.Order;
import com.example.fooddeliverytracking.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CustomerOrdersActivity extends AppCompatActivity {

    private final List<Order> allOrders = new ArrayList<>();
    private boolean showingPast;
    private OrderAdapter orderAdapter;
    private TextView emptyOrders;
    private Query ordersQuery;
    private ValueEventListener ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_orders);
        setTitle(R.string.my_orders);

        emptyOrders = findViewById(R.id.textEmptyOrders);
        orderAdapter = new OrderAdapter(this::openTracking);
        RecyclerView ordersRecycler = findViewById(R.id.recyclerOrders);
        ordersRecycler.setLayoutManager(new LinearLayoutManager(this));
        ordersRecycler.setAdapter(orderAdapter);

        findViewById(R.id.buttonHome).setOnClickListener(view -> {
            Intent intent = new Intent(this, CustomerHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.buttonLogout).setOnClickListener(view ->
                startActivity(new Intent(this, com.example.fooddeliverytracking.AccountActivity.class)
                        .putExtra("role", Constants.ROLE_CUSTOMER)));
        ((com.google.android.material.tabs.TabLayout) findViewById(R.id.tabsOrders)).addOnTabSelectedListener(
                new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                    public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                        showingPast = tab.getPosition() == 1;
                        renderOrders();
                    }
                    public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) { }
                    public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) { }
                });
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            ordersQuery = FirebaseDatabase.getInstance().getReference(Constants.NODE_ORDERS)
                    .orderByChild("customerId").equalTo(user.getUid());
            listenForOrders(user.getUid());
        } else {
            emptyOrders.setVisibility(View.VISIBLE);
        }
    }

    private void listenForOrders(String userId) {
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Order> orders = new ArrayList<>();
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    Order order = orderSnapshot.getValue(Order.class);
                    if (order != null && userId.equals(order.getCustomerId())) {
                        orders.add(order);
                    }
                }
                Collections.sort(orders, Comparator.comparingLong(Order::getCreatedAt).reversed());
                allOrders.clear();
                allOrders.addAll(orders);
                renderOrders();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                emptyOrders.setVisibility(View.VISIBLE);
            }
        };
        ordersQuery.addValueEventListener(ordersListener);
    }

    private void renderOrders() {
        List<Order> visible = new ArrayList<>();
        for (Order order : allOrders) {
            if (Constants.STATUS_DELIVERED.equals(order.getStatus()) == showingPast) visible.add(order);
        }
        orderAdapter.setRecentSection(!showingPast);
        if (!showingPast) {
            int recent = 0;
            for (Order order : allOrders) {
                if (Constants.STATUS_DELIVERED.equals(order.getStatus()) && recent++ < 3) visible.add(order);
            }
        }
        orderAdapter.setOrders(visible);
        emptyOrders.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openTracking(Order order) {
        Intent intent = new Intent(this, OrderTrackingActivity.class);
        intent.putExtra(OrderTrackingActivity.EXTRA_ORDER_ID, order.getOrderId());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersQuery != null && ordersListener != null) {
            ordersQuery.removeEventListener(ordersListener);
        }
    }
}
