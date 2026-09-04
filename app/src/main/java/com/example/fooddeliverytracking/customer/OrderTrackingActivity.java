package com.example.fooddeliverytracking.customer;

import android.animation.ValueAnimator;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.models.DriverLocation;
import com.example.fooddeliverytracking.models.Order;
import com.example.fooddeliverytracking.models.OrderItem;
import com.example.fooddeliverytracking.utils.Constants;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class OrderTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_ORDER_ID = "order_id";

    private GoogleMap googleMap;
    private Marker restaurantMarker;
    private Marker customerMarker;
    private Marker driverMarker;
    private LatLng lastDriverLocation;
    private boolean cameraPositioned;
    private Order currentOrder;
    private DatabaseReference orderReference;
    private ValueEventListener orderListener;
    private TextView[] statusViews;
    private TextView orderIdView;
    private TextView driverView;
    private TextView itemsView;
    private TextView totalView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);
        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderId == null) {
            finish();
            return;
        }
        statusViews = new TextView[]{
                findViewById(R.id.statusPlaced), findViewById(R.id.statusAccepted),
                findViewById(R.id.statusPickedUp), findViewById(R.id.statusOutForDelivery),
                findViewById(R.id.statusDelivered)
        };
        orderIdView = findViewById(R.id.textTrackingOrderId);
        driverView = findViewById(R.id.textTrackingDriver);
        itemsView = findViewById(R.id.textTrackingItems);
        totalView = findViewById(R.id.textTrackingTotal);
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        orderReference = FirebaseDatabase.getInstance().getReference(Constants.NODE_ORDERS).child(orderId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (orderReference == null) {
            return;
        }
        orderListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentOrder = snapshot.getValue(Order.class);
                if (currentOrder != null) {
                    renderOrder();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Keep the last visible order state if Firebase is temporarily unavailable.
            }
        };
        orderReference.addValueEventListener(orderListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (orderReference != null && orderListener != null) {
            orderReference.removeEventListener(orderListener);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        if (currentOrder != null) {
            renderOrder();
        }
    }

    private void renderOrder() {
        String status = currentOrder.getStatus() == null ? Constants.STATUS_PLACED : currentOrder.getStatus();
        updateStatusStepper(status);
        orderIdView.setText(getString(R.string.order_id, currentOrder.getOrderId()));
        String driverName = currentOrder.getDriverName();
        driverView.setText(driverName == null || driverName.isEmpty()
                ? getString(R.string.driver_unassigned) : driverName);
        itemsView.setText(buildItemSummary(currentOrder.getItems()));
        totalView.setText(getString(R.string.order_total, currentOrder.getTotalAmount()));

        if (googleMap == null) {
            return;
        }
        LatLng restaurant = new LatLng(currentOrder.getRestaurantLat(), currentOrder.getRestaurantLng());
        LatLng customer = new LatLng(currentOrder.getCustomerLat(), currentOrder.getCustomerLng());
        if (restaurantMarker == null) {
            restaurantMarker = googleMap.addMarker(new MarkerOptions().position(restaurant)
                    .title(getString(R.string.restaurant)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            customerMarker = googleMap.addMarker(new MarkerOptions().position(customer)
                    .title(getString(R.string.customer_location)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        } else {
            restaurantMarker.setPosition(restaurant);
            customerMarker.setPosition(customer);
        }
        if (currentOrder.getDriverLocation() != null) {
            updateDriverMarker(currentOrder.getDriverLocation());
        }
        if (!cameraPositioned) {
            LatLngBounds.Builder bounds = new LatLngBounds.Builder().include(restaurant).include(customer);
            if (lastDriverLocation != null) {
                bounds.include(lastDriverLocation);
            }
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
            cameraPositioned = true;
        }
    }

    private void updateDriverMarker(DriverLocation location) {
        LatLng target = new LatLng(location.getLatitude(), location.getLongitude());
        if (driverMarker == null) {
            driverMarker = googleMap.addMarker(new MarkerOptions().position(target)
                    .title(getString(R.string.driver_location))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            lastDriverLocation = target;
            return;
        }
        LatLng start = lastDriverLocation == null ? target : lastDriverLocation;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(700);
        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            double latitude = start.latitude + (target.latitude - start.latitude) * progress;
            double longitude = start.longitude + (target.longitude - start.longitude) * progress;
            driverMarker.setPosition(new LatLng(latitude, longitude));
        });
        animator.start();
        lastDriverLocation = target;
    }

    private void updateStatusStepper(String status) {
        int activeStep = 0;
        if (Constants.STATUS_ACCEPTED.equals(status)) activeStep = 1;
        else if (Constants.STATUS_PICKED_UP.equals(status)) activeStep = 2;
        else if (Constants.STATUS_OUT_FOR_DELIVERY.equals(status)) activeStep = 3;
        else if (Constants.STATUS_DELIVERED.equals(status)) activeStep = 4;
        for (int i = 0; i < statusViews.length; i++) {
            boolean active = i <= activeStep;
            statusViews[i].setTextColor(ContextCompat.getColor(this,
                    active ? R.color.primary_orange : R.color.text_secondary));
            statusViews[i].setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private String buildItemSummary(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder summary = new StringBuilder();
        for (OrderItem item : items) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(item.getQuantity()).append("x ").append(item.getName());
        }
        return summary.toString();
    }
}
