package com.example.fooddeliverytracking.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.adapters.OrderAdapter;
import com.example.fooddeliverytracking.models.Order;
import com.example.fooddeliverytracking.models.User;
import com.example.fooddeliverytracking.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverDashboardActivity extends AppCompatActivity {

    private OrderAdapter orderAdapter;
    private TextView emptyText;
    private MaterialButton availableButton;
    private MaterialButton deliveriesButton;
    private FirebaseUser driver;
    private String driverName = "Driver";
    private Query activeQuery;
    private ValueEventListener ordersListener;
    private boolean showingAvailable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);
        driver = FirebaseAuth.getInstance().getCurrentUser();
        if (driver == null) {
            finish();
            return;
        }
        driverName = driver.getEmail() == null ? driverName : driver.getEmail();
        FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS).child(driver.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    User profile = snapshot.getValue(User.class);
                    if (profile != null && profile.getName() != null) driverName = profile.getName();
                });

        emptyText = findViewById(R.id.textEmptyDriverOrders);
        availableButton = findViewById(R.id.buttonAvailableOrders);
        deliveriesButton = findViewById(R.id.buttonMyDeliveries);
        orderAdapter = new OrderAdapter(this::onOrderClicked);
        RecyclerView recyclerView = findViewById(R.id.recyclerDriverOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(orderAdapter);

        availableButton.setOnClickListener(view -> showAvailableOrders());
        deliveriesButton.setOnClickListener(view -> showMyDeliveries());
        showAvailableOrders();
    }

    private void showAvailableOrders() {
        showingAvailable = true;
        orderAdapter.setActionTextRes(R.string.accept_order);
        emptyText.setText(R.string.no_available_orders);
        listenToOrders(FirebaseDatabase.getInstance().getReference(Constants.NODE_ORDERS)
                .orderByChild("status").equalTo(Constants.STATUS_PLACED));
    }

    private void showMyDeliveries() {
        showingAvailable = false;
        orderAdapter.setActionTextRes(R.string.open_delivery);
        emptyText.setText(R.string.no_active_deliveries);
        listenToOrders(FirebaseDatabase.getInstance().getReference(Constants.NODE_ORDERS)
                .orderByChild("driverId").equalTo(driver.getUid()));
    }

    private void listenToOrders(Query query) {
        if (activeQuery != null && ordersListener != null) activeQuery.removeEventListener(ordersListener);
        activeQuery = query;
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Order> orders = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Order order = child.getValue(Order.class);
                    if (order != null && (!showingAvailable || !Constants.STATUS_DELIVERED.equals(order.getStatus()))) {
                        orders.add(order);
                    }
                }
                orderAdapter.setOrders(orders);
                emptyText.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                emptyText.setVisibility(View.VISIBLE);
            }
        };
        activeQuery.addValueEventListener(ordersListener);
    }

    private void onOrderClicked(Order order) {
        if (showingAvailable) {
            acceptOrder(order);
        } else {
            openDelivery(order.getOrderId());
        }
    }

    private void acceptOrder(Order order) {
        Map<String, Object> values = new HashMap<>();
        values.put("driverId", driver.getUid());
        values.put("driverName", driverName);
        values.put("status", Constants.STATUS_ACCEPTED);
        FirebaseDatabase.getInstance().getReference(Constants.NODE_ORDERS).child(order.getOrderId()).updateChildren(values)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, R.string.order_accepted, Toast.LENGTH_SHORT).show();
                        openDelivery(order.getOrderId());
                    }
                });
    }

    private void openDelivery(String orderId) {
        Intent intent = new Intent(this, DriverDeliveryActivity.class);
        intent.putExtra(DriverDeliveryActivity.EXTRA_ORDER_ID, orderId);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeQuery != null && ordersListener != null) activeQuery.removeEventListener(ordersListener);
    }
}
