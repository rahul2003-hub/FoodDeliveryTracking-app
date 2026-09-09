package com.example.fooddeliverytracking;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliverytracking.auth.LoginActivity;
import com.example.fooddeliverytracking.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private final Handler startupHandler = new Handler(Looper.getMainLooper());
    private long startupTime;
    private boolean navigationScheduled;
    private DatabaseReference roleReference;
    private ValueEventListener roleListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startupTime = SystemClock.elapsedRealtime();
        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(getColor(R.color.startup_background));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            getWindow().setNavigationBarColor(getColor(R.color.startup_background));
        }
        routeCurrentUser();
    }

    private void routeCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            openLogin();
            return;
        }

        roleReference = FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS)
                .child(user.getUid()).child("role");
        roleListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);
                if (Constants.ROLE_DRIVER.equals(role)) {
                    openRoleScreen(".driver.DriverDashboardActivity");
                } else if (Constants.ROLE_CUSTOMER.equals(role)) {
                    openRoleScreen(".customer.CustomerHomeActivity");
                } else {
                    FirebaseAuth.getInstance().signOut();
                    openLogin();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                openLogin();
            }
        };
        roleReference.addListenerForSingleValueEvent(roleListener);
    }

    private void openLogin() {
        openWhenReady(new Intent(this, LoginActivity.class));
    }

    private void openRoleScreen(String activityName) {
        Intent intent = new Intent();
        intent.setClassName(this, getPackageName() + activityName);
        openWhenReady(intent);
    }

    private void openWhenReady(Intent intent) {
        if (navigationScheduled || isFinishing() || isDestroyed()) return;
        navigationScheduled = true;
        // Resolve the session immediately; allow the startup artwork a brief first frame.
        // startup loading screen time animation | scooter illustration
        long remaining = Math.max(0, 3000 - (SystemClock.elapsedRealtime() - startupTime));
        startupHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            startActivity(intent);
            finish();
        }, remaining);
    }

    @Override
    protected void onDestroy() {
        startupHandler.removeCallbacksAndMessages(null);
        if (roleReference != null && roleListener != null) {
            roleReference.removeEventListener(roleListener);
        }
        super.onDestroy();
    }
}
