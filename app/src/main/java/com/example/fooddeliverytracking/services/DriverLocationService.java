package com.example.fooddeliverytracking.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.driver.DriverDeliveryActivity;
import com.example.fooddeliverytracking.models.DriverLocation;
import com.example.fooddeliverytracking.utils.Constants;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLocationService extends Service {

    private static final String CHANNEL_ID = "driver_location";
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private String orderId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        orderId = intent == null ? null : intent.getStringExtra(DriverDeliveryActivity.EXTRA_ORDER_ID);
        if (orderId == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        createNotificationChannel();
        startForeground(1, new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.live_location_notification))
                .setContentText(getString(R.string.live_location_notification_text))
                .setOngoing(true)
                .build());
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startLocationUpdates();
        return START_NOT_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000).build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result.getLastLocation() == null) return;
                DriverLocation location = new DriverLocation(result.getLastLocation().getLatitude(),
                        result.getLastLocation().getLongitude(), System.currentTimeMillis());
                FirebaseDatabase.getInstance().getReference(Constants.NODE_ORDERS).child(orderId)
                        .child("driverLocation").setValue(location);
            }
        };
        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.live_location_notification), NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
