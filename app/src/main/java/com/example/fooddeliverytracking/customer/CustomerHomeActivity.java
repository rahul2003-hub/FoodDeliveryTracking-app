package com.example.fooddeliverytracking.customer;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.adapters.MenuAdapter;
import com.example.fooddeliverytracking.auth.LoginActivity;
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

    private List<MenuItem> allMenuItems = Arrays.asList(
            new MenuItem("burger", "Classic Burger", "Grilled patty with fresh vegetables", 120, R.drawable.ic_burger),
            new MenuItem("pizza", "Veg Pizza", "Cheesy garden vegetable pizza", 180, R.drawable.ic_pizza),
            new MenuItem("pasta", "Creamy Pasta", "Pasta in a rich white sauce", 150, R.drawable.ic_pasta),
            new MenuItem("drink", "Cold Drink", "Chilled soft beverage", 40, R.drawable.ic_drink)
    );

    private MenuAdapter menuAdapter;
    private TextInputEditText addressInput;
    private TextView cartTotal;
    private String customerName = "Customer";
    private String restaurantName;
    private String restaurantAddress;

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
        ((MaterialButton) findViewById(R.id.buttonCancelOrder)).setOnClickListener(view -> cancelDraft());
        ((MaterialButton) findViewById(R.id.buttonMyOrders)).setOnClickListener(view ->
                startActivity(new Intent(this, CustomerOrdersActivity.class)));
        ((MaterialButton) findViewById(R.id.buttonLogout)).setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
        ((Spinner) findViewById(R.id.spinnerRestaurant)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectRestaurant(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        loadCustomerName();
        selectRestaurant(0);
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

        new Thread(() -> findAddressAndPlaceOrder(user, items, address, restaurantName, restaurantAddress)).start();
    }

    private void findAddressAndPlaceOrder(FirebaseUser user, List<OrderItem> items, String address,
                                          String selectedRestaurant, String selectedRestaurantAddress) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> customerResults = geocoder.getFromLocationName(address, 1);
            List<Address> restaurantResults = geocoder.getFromLocationName(selectedRestaurantAddress, 1);
            if (customerResults == null || customerResults.isEmpty()
                    || restaurantResults == null || restaurantResults.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, R.string.address_not_found, Toast.LENGTH_SHORT).show());
                return;
            }
            Address customer = customerResults.get(0);
            Address restaurant = restaurantResults.get(0);
            runOnUiThread(() -> saveOrder(user, items, address, customer.getLatitude(), customer.getLongitude(),
                    selectedRestaurant, selectedRestaurantAddress, restaurant.getLatitude(), restaurant.getLongitude()));
        } catch (Exception error) {
            runOnUiThread(() -> Toast.makeText(this, R.string.address_not_found, Toast.LENGTH_SHORT).show());
        }
    }

    private void saveOrder(FirebaseUser user, List<OrderItem> items, String address, double latitude, double longitude,
                           String selectedRestaurant, String selectedRestaurantAddress,
                           double restaurantLatitude, double restaurantLongitude) {
        String orderId = FirebaseDatabase.getInstance().getReference(Constants.NODE_ORDERS).push().getKey();
        if (orderId == null) {
            Toast.makeText(this, getString(R.string.auth_failed, ""), Toast.LENGTH_SHORT).show();
            return;
        }
        Order order = new Order(orderId, user.getUid(), customerName, address, new ArrayList<>(items));
        order.setRestaurantName(selectedRestaurant);
        order.setRestaurantAddress(selectedRestaurantAddress);
        order.setRestaurantLat(restaurantLatitude);
        order.setRestaurantLng(restaurantLongitude);
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

    private void cancelDraft() {
        menuAdapter.clearCart();
        addressInput.setText("");
    }

    private void selectRestaurant(int position) {
        switch (position) {
            case 1:
                setRestaurant("ONECOOK - Chinese Restaurant", "Opposite Navjivan Society, Chembur Camp, Ashok Nagar, Collector Colony, Chembur, Mumbai, Maharashtra 400074",
                        "Chicken Hakka Noodles", "Veg Manchurian", "Chicken Fried Rice", "Lemon Iced Tea");
                break;
            case 2:
                setRestaurant("31441 Pizzeria Chembur", "Shop No. 8, Ground Floor, Chhadva Apartments, Sion - Trombay Road, Borla, Union Park, Chembur, Mumbai, Maharashtra 400071",
                        "Margherita Pizza", "Farmhouse Pizza", "Chicken Pizza", "Cold Coffee");
                break;
            case 3:
                setRestaurant("Sawali Restaurant (Pure Veg)", "Shop No. 11, Angulimala Co-Operative Society, B-Wing, SG Barve Marg, Nehru Nagar, Kurla East, Mumbai, Maharashtra 400024",
                        "Paneer Tikka Masala", "Pav Bhaji", "Veg Pulao", "Sweet Lassi");
                break;
            case 4:
                setRestaurant("KFC", "Shop No 14B, Ground Floor, East Point Market, Jagruti Nagar, Police Colony, Kurla, Mumbai, Maharashtra 400024",
                        "Veg Zinger Burger", "Chicken Bucket", "Chicken Popcorn", "Pepsi");
                break;
            case 5:
                setRestaurant("Gurukripa", "40, Road Number 24, Near SIES College Of Arts, Science and Commerce, Sion West, Mumbai, Maharashtra 400022",
                        "Samosa Chole", "Chole Bhature", "Veg Pulao", "Sweet Lassi");
                break;
            default:
                setRestaurant("Marathi Mejvani", "Marathi Mejvani, Collector Colony, Chembur, Mumbai, Maharashtra 400071",
                        "Chicken Thali", "Mutton Thali", "Veg Thali", "Solkadhi");
        }
    }

    private void setRestaurant(String name, String address, String firstItem, String secondItem,
                               String thirdItem, String drink) {
        restaurantName = name;
        restaurantAddress = address;
        allMenuItems = Arrays.asList(
                new MenuItem(name + "_1", firstItem, "Restaurant special", 180, R.drawable.ic_pasta),
                new MenuItem(name + "_2", secondItem, "Freshly prepared", 160, R.drawable.ic_pizza),
                new MenuItem(name + "_3", thirdItem, "Popular choice", 220, R.drawable.ic_burger),
                new MenuItem(name + "_4", drink, "Refreshing beverage", 60, R.drawable.ic_drink));
        menuAdapter.clearCart();
        menuAdapter.setMenuItems(allMenuItems);
    }
}
