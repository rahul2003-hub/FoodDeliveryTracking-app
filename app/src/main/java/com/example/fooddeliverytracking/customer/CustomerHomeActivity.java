package com.example.fooddeliverytracking.customer;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.adapters.MenuAdapter;
import com.example.fooddeliverytracking.models.MenuItem;
import com.example.fooddeliverytracking.models.Order;
import com.example.fooddeliverytracking.models.OrderItem;
import com.example.fooddeliverytracking.models.User;
import com.example.fooddeliverytracking.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CustomerHomeActivity extends AppCompatActivity {

    private static final double RESTAURANT_LAT = 12.9716;
    private static final double RESTAURANT_LNG = 77.5946;

    private final List<MenuItem> allMenuItems = Arrays.asList(
            new MenuItem("burger", "Classic Burger", "Grilled patty with fresh vegetables", 120, R.drawable.ic_burger),
            new MenuItem("pizza", "Veg Pizza", "Cheesy garden vegetable pizza", 180, R.drawable.ic_pizza),
            new MenuItem("pasta", "Creamy Pasta", "Pasta in a rich white sauce", 150, R.drawable.ic_pasta),
            new MenuItem("drink", "Cold Drink", "Chilled soft beverage", 40, R.drawable.ic_drink)
    );

    private MenuAdapter menuAdapter;
    private TextInputEditText addressInput;
    private TextView cartTotal;
    private String customerName = "Customer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        addressInput = findViewById(R.id.editTextAddress);
        cartTotal = findViewById(R.id.textCartTotal);
        menuAdapter = new MenuAdapter(allMenuItems, this::updateCartSummary);
        RecyclerView menuRecycler = findViewById(R.id.recyclerMenu);
        menuRecycler.setLayoutManager(new LinearLayoutManager(this));
        menuRecycler.setAdapter(menuAdapter);

        ((ChipGroup) findViewById(R.id.chipGroupCategory)).setOnCheckedStateChangeListener((group, checkedIds) -> {
            int selectedId = checkedIds.isEmpty() ? R.id.chipAll : checkedIds.get(0);
            if (selectedId == R.id.chipDrinks) {
                menuAdapter.setMenuItems(Arrays.asList(allMenuItems.get(3)));
            } else if (selectedId == R.id.chipMeals) {
                menuAdapter.setMenuItems(allMenuItems.subList(0, 3));
            } else {
                menuAdapter.setMenuItems(allMenuItems);
            }
        });
        ((MaterialButton) findViewById(R.id.buttonPlaceOrder)).setOnClickListener(view -> placeOrder());
        ((MaterialButton) findViewById(R.id.buttonMyOrders)).setOnClickListener(view ->
                startActivity(new Intent(this, CustomerOrdersActivity.class)));
        loadCustomerName();
        updateCartSummary();
    }

    private void loadCustomerName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        customerName = user.getEmail() == null ? customerName : user.getEmail();
        FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS).child(user.getUid())
                .get().addOnSuccessListener(snapshot -> {
                    User profile = snapshot.getValue(User.class);
                    if (profile != null && !TextUtils.isEmpty(profile.getName())) {
                        customerName = profile.getName();
                    }
                });
    }

    private void updateCartSummary() {
        cartTotal.setText(getString(R.string.cart_summary, menuAdapter.getCartItemCount(), menuAdapter.getCartTotal()));
    }

    private void placeOrder() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        List<OrderItem> items = menuAdapter.getOrderItems();
        String address = addressInput.getText() == null ? "" : addressInput.getText().toString().trim();
        if (user == null || items.isEmpty()) {
            Toast.makeText(this, R.string.empty_cart, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(address)) {
            addressInput.setError(getString(R.string.field_required, getString(R.string.delivery_address)));
            return;
        }

        new Thread(() -> findAddressAndPlaceOrder(user, items, address)).start();
    }

    private void findAddressAndPlaceOrder(FirebaseUser user, List<OrderItem> items, String address) {
        try {
            List<Address> results = new Geocoder(this, Locale.getDefault()).getFromLocationName(address, 1);
            if (results == null || results.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, R.string.address_not_found, Toast.LENGTH_SHORT).show());
                return;
            }
            Address result = results.get(0);
            runOnUiThread(() -> saveOrder(user, items, address, result.getLatitude(), result.getLongitude()));
        } catch (Exception error) {
            runOnUiThread(() -> Toast.makeText(this, R.string.address_not_found, Toast.LENGTH_SHORT).show());
        }
    }

    private void saveOrder(FirebaseUser user, List<OrderItem> items, String address, double latitude, double longitude) {
        String orderId = FirebaseDatabase.getInstance().getReference(Constants.NODE_ORDERS).push().getKey();
        if (orderId == null) {
            Toast.makeText(this, getString(R.string.auth_failed, ""), Toast.LENGTH_SHORT).show();
            return;
        }
        Order order = new Order(orderId, user.getUid(), customerName, address, new ArrayList<>(items));
        order.setRestaurantName("Campus Food Corner");
        order.setRestaurantAddress("College Campus");
        order.setRestaurantLat(RESTAURANT_LAT);
        order.setRestaurantLng(RESTAURANT_LNG);
        order.setCustomerLat(latitude);
        order.setCustomerLng(longitude);
        order.setCreatedAt(System.currentTimeMillis());

        FirebaseDatabase.getInstance().getReference(Constants.NODE_ORDERS).child(orderId).setValue(order)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, getString(R.string.auth_failed,
                                task.getException() == null ? "" : task.getException().getMessage()), Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(this, R.string.order_placed, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, OrderTrackingActivity.class);
                    intent.putExtra(OrderTrackingActivity.EXTRA_ORDER_ID, orderId);
                    startActivity(intent);
                });
    }
}
