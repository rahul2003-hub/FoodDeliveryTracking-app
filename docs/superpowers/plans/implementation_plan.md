# Food Delivery Tracking Application Implementation Plan

A complete, clean, academic-standard Android application written in **Java + XML** demonstrating real-time mobile computing:
- **Firebase Authentication** with Customer & Driver role management.
- **Firebase Realtime Database** for live order status and GPS synchronization.
- **Google Maps Android SDK** for visual tracking with restaurant, customer, and driver markers.
- **Fused Location Provider API & Android Foreground Service** for live driver GPS broadcasting.

## Proposed Changes

---

### Phase 1: Core Models & Constants
- Define system constants (`STATUS_PLACED`, `STATUS_ACCEPTED`, `STATUS_PICKED_UP`, `STATUS_OUT_FOR_DELIVERY`, `STATUS_DELIVERED`, `ROLE_CUSTOMER`, `ROLE_DRIVER`).
- Implement Firebase POJO models:
  - `User.java`: User profile with UID, name, email, role.
  - `MenuItem.java`: Restaurant menu items (name, description, price, icon).
  - `OrderItem.java`: Ordered item with quantity.
  - `DriverLocation.java`: Latitude, longitude, updatedAt timestamp.
  - `Order.java`: Comprehensive order object connecting customer, restaurant, items, driver, status, and driver location.
- Implement Model unit tests in JUnit to verify calculations and model integrity.

---

### Phase 2: Resources, Theme & AndroidManifest Configuration
- Add colors, themes, and Material typography.
- Add vector icons for food categories, markers, and statuses.
- Update `AndroidManifest.xml` with permissions (`INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`) and declare all Activities and Services.

---

### Phase 3: Authentication & Role-Based Routing
- `activity_login.xml` & `LoginActivity.java`: Authenticate with Firebase Auth, read `/users/{uid}/role`, and route to Customer or Driver dashboard.
- `activity_register.xml` & `RegisterActivity.java`: Register new user with Role selection (`Customer` vs `Delivery Driver`) and write user profile to `/users/{uid}`.
- Update `MainActivity.java` to act as splash/session router.

---

### Phase 4: Customer Menu, Cart & Order Placement
- `item_menu.xml` & `MenuAdapter.java`: Menu card design with quantity controls and real-time total updates.
- `activity_customer_home.xml` & `CustomerHomeActivity.java`: Menu catalog with category chips, address input, floating checkout bar, and order submission to Firebase `/orders/{orderId}`.

---

### Phase 5: Customer Order History & Live Map Tracking Screen
- `item_order.xml` & `OrderAdapter.java`: Order card showing date, items, total, and live status badge.
- `activity_customer_orders.xml` & `CustomerOrdersActivity.java`: Filtered list of past and active orders.
- `activity_order_tracking.xml` & `OrderTrackingActivity.java`: 
  - Visual status progress stepper.
  - `SupportMapFragment` with markers for Restaurant, Customer delivery address, and Driver.
  - Real-time `ValueEventListener` attached to `/orders/{orderId}` updating status and smoothly moving the driver's motorcycle marker.

---

### Phase 6: Driver Dashboard & Delivery Management
- `activity_driver_dashboard.xml` & `DriverDashboardActivity.java`: 
  - "Available Orders" pool where drivers can claim unassigned orders (`status == PLACED`).
  - "My Deliveries" tab for active orders.
- `activity_driver_delivery.xml` & `DriverDeliveryActivity.java`:
  - Destination details, customer contact, and delivery steps.
  - Status transition buttons (`Mark Picked Up` ➔ `Start Delivery` ➔ `Mark Delivered`).
  - Emulator simulation step button for indoor testing without physical movement.

---

### Phase 7: Driver Background Location Service
- `DriverLocationService.java`: Standard Android `ForegroundService` with ongoing notification.
- Connects `FusedLocationProviderClient` to request high-accuracy location every 5 seconds and push coordinates directly to Firebase Realtime Database at `/orders/{orderId}/driverLocation`.

---

## Verification Plan

### Automated Verification
- Run `./gradlew testDebugUnitTest` to verify data models and business logic.
- Run `./gradlew assembleDebug` to ensure all Java classes, layouts, and manifest entries compile cleanly into an APK.

### Manual Verification Flow
1. **Customer Side:** Register as a customer, add food items to cart, enter address, place order.
2. **Driver Side:** Register/login as driver on another device/emulator or switch user, see the placed order in "Available Orders", accept it.
3. **Tracking & GPS:** Advance status to `OUT_FOR_DELIVERY` on driver side, verify customer's map immediately receives live coordinates and status updates.
