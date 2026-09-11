package com.example.fooddeliverytracking;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import com.example.fooddeliverytracking.auth.LoginActivity;
import com.example.fooddeliverytracking.customer.CustomerHomeActivity;
import com.example.fooddeliverytracking.customer.CustomerOrdersActivity;
import com.example.fooddeliverytracking.driver.DriverDashboardActivity;
import com.example.fooddeliverytracking.utils.Constants;
import com.example.fooddeliverytracking.utils.DeliveryUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class AccountActivity extends AppCompatActivity {
    private DatabaseReference profileRef;
    private String name = "", phone = "", address = "", role;
    private MaterialSwitch notifications;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_account);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        role = getIntent().getStringExtra("role");
        ((TextView) findViewById(R.id.textAccountEmail)).setText(user.getEmail());
        name = user.getDisplayName() == null ? "" : user.getDisplayName();
        profileRef = FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS).child(user.getUid());
        profileRef.get().addOnSuccessListener(snapshot -> {
            if (isDestroyed()) return;
            name = value(snapshot.child("name").getValue(String.class));
            phone = value(snapshot.child("phone").getValue(String.class));
            address = value(snapshot.child("homeAddress").getValue(String.class));
            role = snapshot.child("role").getValue(String.class);
            render();
        }).addOnFailureListener(this::showError);
        notifications = findViewById(R.id.switchNotifications);
        notifications.setOnClickListener(v -> openNotificationSettings());
        findViewById(R.id.notificationRow).setOnClickListener(v -> openNotificationSettings());
        findViewById(R.id.buttonEditProfile).setOnClickListener(v -> edit(false));
        findViewById(R.id.buttonProfile).setOnClickListener(v -> edit(false));
        findViewById(R.id.textAccountPhone).setOnClickListener(v -> edit(false));
        findViewById(R.id.textAccountAddress).setOnClickListener(v -> edit(true));
        findViewById(R.id.buttonAddresses).setOnClickListener(v -> edit(true));
        findViewById(R.id.buttonPayments).setOnClickListener(v ->
                info(R.string.payment_methods, getString(R.string.cash_on_delivery) + "\n" + getString(R.string.pay_on_arrival)));
        findViewById(R.id.buttonHelp).setOnClickListener(v -> info(R.string.help_support, getString(R.string.profile_help)));
        findViewById(R.id.buttonAbout).setOnClickListener(v ->
                info(R.string.about_app, getString(R.string.app_name) + "\n" + version()));
        findViewById(R.id.buttonSignOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
        findViewById(R.id.buttonHome).setOnClickListener(v -> navigate(false));
        findViewById(R.id.buttonMyOrders).setOnClickListener(v -> navigate(true));
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notifications != null) notifications.setChecked(NotificationManagerCompat.from(this).areNotificationsEnabled());
    }

    private void render() {
        ((TextView) findViewById(R.id.textAccountName)).setText(name.isEmpty() ? getString(R.string.account) : name);
        DeliveryUi.avatar(findViewById(R.id.textAccountAvatar), name);
        ((TextView) findViewById(R.id.textAccountPhone)).setText(phone.isEmpty() ? getString(R.string.add_phone) : phone);
        ((TextView) findViewById(R.id.textAccountAddress)).setText(address.isEmpty() ? getString(R.string.add_address) : address);
        ((TextView) findViewById(R.id.textSavedAddress)).setText(address.isEmpty() ? getString(R.string.add_address) : address);
        ((TextView) findViewById(R.id.textPaymentMethod)).setText(R.string.cash_on_delivery);
        ((TextView) findViewById(R.id.textAppVersion)).setText(version());
        boolean driver = Constants.ROLE_DRIVER.equals(role);
        ((MaterialButton) findViewById(R.id.buttonHome)).setText(driver ? R.string.deliveries : R.string.home);
        ((MaterialButton) findViewById(R.id.buttonMyOrders)).setText(driver ? R.string.history : R.string.orders);
    }

    private void edit(boolean addressesOnly) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        form.setPadding(pad, 0, pad, 0);
        TextInputEditText nameInput = addressesOnly ? null : input(form, R.string.full_name, name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        TextInputEditText phoneInput = addressesOnly ? null : input(form, R.string.phone_number, phone, InputType.TYPE_CLASS_PHONE);
        TextInputEditText addressInput = input(form, R.string.delivery_address, address, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(addressesOnly ? R.string.saved_addresses : R.string.my_profile).setView(form)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(v -> {
            String nextName = addressesOnly ? name : value(nameInput.getText().toString());
            if (!addressesOnly && nextName.isEmpty()) { nameInput.setError(getString(R.string.field_required, getString(R.string.full_name))); return; }
            String nextPhone = addressesOnly ? phone : value(phoneInput.getText().toString());
            String nextAddress = value(addressInput.getText().toString());
            Map<String, Object> values = new HashMap<>();
            if (!addressesOnly) { values.put("name", nextName); values.put("phone", nextPhone); }
            values.put("homeAddress", nextAddress);
            dialog.getButton(-1).setEnabled(false);
            profileRef.updateChildren(values).addOnSuccessListener(unused -> {
                name = nextName; phone = nextPhone; address = nextAddress;
                render();
                dialog.dismiss();
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(error -> { dialog.getButton(-1).setEnabled(true); showError(error); });
        }));
        dialog.show();
    }

    private TextInputEditText input(LinearLayout form, int hint, String value, int type) {
        TextInputLayout field = new TextInputLayout(this);
        field.setHint(getString(hint));
        TextInputEditText input = new TextInputEditText(this);
        input.setInputType(type);
        input.setText(value);
        field.addView(input);
        form.addView(field, new LinearLayout.LayoutParams(-1, -2));
        return input;
    }

    private void openNotificationSettings() {
        Intent intent = Build.VERSION.SDK_INT >= 26
                ? new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                : new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void navigate(boolean orders) {
        boolean driver = Constants.ROLE_DRIVER.equals(role);
        Intent intent = new Intent(this, driver ? DriverDashboardActivity.class
                : orders ? CustomerOrdersActivity.class : CustomerHomeActivity.class);
        if (driver) intent.putExtra("show_history", orders);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String version() {
        try { return getString(R.string.version_label, getPackageManager().getPackageInfo(getPackageName(), 0).versionName); }
        catch (android.content.pm.PackageManager.NameNotFoundException ignored) { return getString(R.string.app_name); }
    }

    private void info(int title, String message) {
        new MaterialAlertDialogBuilder(this).setTitle(title).setMessage(message).setPositiveButton(android.R.string.ok, null).show();
    }

    private void showError(Exception error) {
        Toast.makeText(this, getString(R.string.auth_failed, error.getLocalizedMessage()), Toast.LENGTH_LONG).show();
    }

    private String value(String value) { return value == null ? "" : value.trim(); }
}
