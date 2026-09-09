package com.example.fooddeliverytracking.driver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.models.Order;
import com.example.fooddeliverytracking.models.OrderItem;
import com.example.fooddeliverytracking.services.DriverLocationService;
import com.example.fooddeliverytracking.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.example.fooddeliverytracking.models.DriverLocation;

import java.util.List;

public class DriverDeliveryActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    private static final int LOCATION_PERMISSION_REQUEST = 10;

    private String orderId;
    private Order currentOrder;
    private DatabaseReference orderReference;
    private ValueEventListener orderListener;
    private TextView orderIdText;
    private TextView statusText;
    private TextView customerText;
    private TextView destinationText;
    private TextView itemsText;
    private TextView totalText;
    private MaterialButton pickedUpButton;
    private MaterialButton startDeliveryButton;
    private MaterialButton deliveredButton;
    private GoogleMap deliveryMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_delivery);
        findViewById(R.id.buttonBack).setOnClickListener(view -> finish());
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.driverMap))
                .getMapAsync(map -> { deliveryMap = map; if (currentOrder != null) renderMap(); });
        findViewById(R.id.buttonNavigation).setOnClickListener(view -> {
            if (currentOrder == null) return;
            boolean pickup = Constants.STATUS_ACCEPTED.equals(currentOrder.getStatus());
            String destination = pickup ? currentOrder.getRestaurantAddress() : currentOrder.getCustomerAddress();
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(destination)));
            try { startActivity(intent); } catch (android.content.ActivityNotFoundException error) {
                startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + android.net.Uri.encode(destination))));
            }
        });
        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderId == null) {
            finish();
            return;
        }
        orderIdText = findViewById(R.id.textDriverOrderId);
        statusText = findViewById(R.id.textDriverStatus);
        customerText = findViewById(R.id.textDriverCustomer);
        destinationText = findViewById(R.id.textDriverDestination);
        itemsText = findViewById(R.id.textDriverItems);
        totalText = findViewById(R.id.textDriverTotal);
        pickedUpButton = findViewById(R.id.buttonPickedUp);
        startDeliveryButton = findViewById(R.id.buttonStartDelivery);
        deliveredButton = findViewById(R.id.buttonDelivered);
        pickedUpButton.setOnClickListener(view -> updateStatus(Constants.STATUS_PICKED_UP));
        startDeliveryButton.setOnClickListener(view -> startDelivery());
        deliveredButton.setOnClickListener(view -> updateStatus(Constants.STATUS_DELIVERED));
        orderReference = FirebaseDatabase.getInstance().getReference(Constants.NODE_ORDERS).child(orderId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (orderReference == null) return;
        orderListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                currentOrder = snapshot.getValue(Order.class);
                if (currentOrder != null) renderOrder();
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        };
        orderReference.addValueEventListener(orderListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (orderListener != null) orderReference.removeEventListener(orderListener);
    }

    private void renderOrder() {
        String status = currentOrder.getStatus();
        orderIdText.setText(getString(R.string.order_id, currentOrder.getOrderId()));
        statusText.setText(getString(R.string.status, status == null ? "" : status.replace('_', ' ')));
        customerText.setText(getString(R.string.customer_details, currentOrder.getCustomerName()));
        destinationText.setText(getString(R.string.destination, currentOrder.getCustomerAddress()));
        itemsText.setText(buildItemSummary(currentOrder.getItems()));
        totalText.setText(getString(R.string.cash_collect, currentOrder.getTotalAmount()));
        ((TextView) findViewById(R.id.textDriverPickup)).setText(getString(R.string.pickup_from, currentOrder.getRestaurantName()));
        pickedUpButton.setEnabled(Constants.STATUS_ACCEPTED.equals(status));
        startDeliveryButton.setEnabled(Constants.STATUS_PICKED_UP.equals(status));
        deliveredButton.setEnabled(Constants.STATUS_OUT_FOR_DELIVERY.equals(status));
        pickedUpButton.setVisibility(Constants.STATUS_ACCEPTED.equals(status) ? View.VISIBLE : View.GONE);
        startDeliveryButton.setVisibility(Constants.STATUS_PICKED_UP.equals(status) ? View.VISIBLE : View.GONE);
        deliveredButton.setVisibility(Constants.STATUS_OUT_FOR_DELIVERY.equals(status) ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonNavigation).setVisibility(Constants.STATUS_DELIVERED.equals(status) ? View.GONE : View.VISIBLE);
        renderMap();
    }

    private void renderMap() {
        if (deliveryMap == null || currentOrder == null) return;
        deliveryMap.clear();
        LatLng pickup = new LatLng(currentOrder.getRestaurantLat(), currentOrder.getRestaurantLng());
        LatLng dropoff = new LatLng(currentOrder.getCustomerLat(), currentOrder.getCustomerLng());
        deliveryMap.addMarker(new MarkerOptions().position(pickup).title(currentOrder.getRestaurantName()));
        deliveryMap.addMarker(new MarkerOptions().position(dropoff).title(currentOrder.getCustomerAddress()));
        LatLngBounds.Builder bounds = new LatLngBounds.Builder().include(pickup).include(dropoff);
        if (currentOrder.getDriverLocation() != null) {
            DriverLocation location = currentOrder.getDriverLocation();
            LatLng driver = new LatLng(location.getLatitude(), location.getLongitude());
            deliveryMap.addMarker(new MarkerOptions().position(driver).title(getString(R.string.driver_location))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            bounds.include(driver);
        }
        deliveryMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 64));
    }

    private void startDelivery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestDeliveryPermissions();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestDeliveryPermissions();
            return;
        }
        updateStatus(Constants.STATUS_OUT_FOR_DELIVERY);
    }

    private void requestDeliveryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateStatus(Constants.STATUS_OUT_FOR_DELIVERY);
        } else {
            Toast.makeText(this, R.string.location_permission_needed, Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus(String status) {
        orderReference.child("status").setValue(status).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            if (Constants.STATUS_OUT_FOR_DELIVERY.equals(status)) startLocationService();
            if (Constants.STATUS_DELIVERED.equals(status)) stopService(new Intent(this, DriverLocationService.class));
        });
    }

    private void startLocationService() {
        Intent intent = new Intent(this, DriverLocationService.class);
        intent.putExtra(EXTRA_ORDER_ID, orderId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    private String buildItemSummary(List<OrderItem> items) {
        StringBuilder summary = new StringBuilder();
        if (items != null) for (OrderItem item : items) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(item.getQuantity()).append("x ").append(item.getName());
        }
        return summary.toString();
    }
}
