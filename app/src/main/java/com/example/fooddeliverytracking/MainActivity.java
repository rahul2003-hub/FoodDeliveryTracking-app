package com.example.fooddeliverytracking;

import android.content.Intent;
import android.os.Bundle;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        routeCurrentUser();
    }

    private void routeCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            openLogin();
            return;
        }

        DatabaseReference roleReference = FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS)
                .child(user.getUid()).child("role");
        roleReference.addListenerForSingleValueEvent(new ValueEventListener() {
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
        });
    }

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void openRoleScreen(String activityName) {
        Intent intent = new Intent();
        intent.setClassName(this, getPackageName() + activityName);
        startActivity(intent);
        finish();
    }
}
