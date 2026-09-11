package com.example.fooddeliverytracking.customer;

import android.animation.ValueAnimator;
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
import com.example.fooddeliverytracking.utils.DeliveryUi;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
    private String contactId;
    private TextView orderIdView;
    private TextView driverView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);
        findViewById(R.id.buttonBack).setOnClickListener(view -> finish());
        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderId == null) {
            finish();
            return;
        }
        orderIdView = findViewById(R.id.textTrackingOrderId);
        driverView = findViewById(R.id.textTrackingDriver);
        findViewById(R.id.buttonRecenter).setOnClickListener(view -> {
            cameraPositioned = false;
            if (currentOrder != null) renderOrder();
        });
        findViewById(R.id.buttonOrderDetails).setOnClickListener(view -> {
            if (currentOrder == null) return;
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.order_details)
                    .setMessage(currentOrder.getRestaurantName() + "\n"
                            + buildItemSummary(currentOrder.getItems()) + "\n"
                            + getString(R.string.order_total, currentOrder.getTotalAmount()) + "\n"
                            + currentOrder.getCustomerAddress())
                    .setPositiveButton(android.R.string.ok, null).show();
        });
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
        DeliveryUi.progress(findViewById(android.R.id.content), status, false);
        TextView badge = findViewById(R.id.textTrackingStatus);
        badge.setText(DeliveryUi.statusLabel(status));
        DeliveryUi.badge(badge, Constants.STATUS_OUT_FOR_DELIVERY.equals(status) || Constants.STATUS_DELIVERED.equals(status));
        ((TextView) findViewById(R.id.textEta)).setText(DeliveryUi.arrival(this, currentOrder));
        ((TextView) findViewById(R.id.textEtaCaption)).setText(currentOrder.getEstimatedArrivalAt() == null
                ? R.string.estimate_pending : R.string.estimated_arrival);
        findViewById(R.id.textEtaCaption).setVisibility(Constants.STATUS_DELIVERED.equals(status)
                ? android.view.View.GONE : android.view.View.VISIBLE);
        orderIdView.setText(getString(R.string.short_order_id, currentOrder.getOrderId()));
        String driverName = currentOrder.getDriverName();
        driverView.setText(driverName == null || driverName.isEmpty()
                ? getString(R.string.driver_unassigned) : driverName);
        DeliveryUi.avatar(findViewById(R.id.textDriverAvatar), driverName);
        if (!java.util.Objects.equals(contactId, currentOrder.getDriverId())) {
            contactId = currentOrder.getDriverId();
            DeliveryUi.contact(this, findViewById(R.id.buttonCallContact), contactId);
        }

        TextView locationUpdated = findViewById(R.id.textLocationUpdated);
        DriverLocation location = currentOrder.getDriverLocation();
        locationUpdated.setText(location == null ? getString(R.string.waiting_location)
                : getString(R.string.location_updated, android.text.format.DateUtils.getRelativeTimeSpanString(
                        location.getUpdatedAt(), System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS)));
        locationUpdated.setTextColor(ContextCompat.getColor(this,
                location != null && System.currentTimeMillis() - location.getUpdatedAt() < 60000
                        ? R.color.accent_green : R.color.text_secondary));
        if (googleMap == null) {
            return;
        }
        LatLng restaurant = new LatLng(currentOrder.getRestaurantLat(), currentOrder.getRestaurantLng());
        LatLng customer = new LatLng(currentOrder.getCustomerLat(), currentOrder.getCustomerLng());
        if (restaurantMarker == null) {
            restaurantMarker = googleMap.addMarker(new MarkerOptions().position(restaurant)
                    .title(currentOrder.getRestaurantName()).icon(DeliveryUi.marker(this, R.drawable.ic_restaurant, currentOrder.getRestaurantName())));
            customerMarker = googleMap.addMarker(new MarkerOptions().position(customer)
                    .title(currentOrder.getCustomerAddress()).icon(DeliveryUi.marker(this, R.drawable.ic_destination, getString(R.string.home))));
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
            findViewById(R.id.map).post(() -> {
                if (!isDestroyed() && googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
                }
            });
            cameraPositioned = true;
        }
    }

    private void updateDriverMarker(DriverLocation location) {
        LatLng target = new LatLng(location.getLatitude(), location.getLongitude());
        if (driverMarker == null) {
            driverMarker = googleMap.addMarker(new MarkerOptions().position(target)
                    .title(getString(R.string.driver_location))
                    .icon(DeliveryUi.marker(this, R.drawable.ic_motorcycle, getString(R.string.driver_location))));
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
