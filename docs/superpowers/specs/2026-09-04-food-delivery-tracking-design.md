# Food Delivery Tracking Application Specification (MCA Mini Project)

## 1. Overview & Objective
An academic, clean, and practical Android application written in **Java + XML** demonstrating real-time mobile computing:
- **Firebase Authentication** for user login, signup, and role management (Customer vs. Delivery Driver).
- **Firebase Realtime Database** for real-time state synchronization (order status and live driver GPS coordinates).
- **Google Maps Android SDK** for visual tracking of restaurant, customer, and driver markers.
- **Fused Location Provider API & Android Service** for driver background GPS updates.

---

## 2. User Roles & Workflows

### 2.1 Customer Workflow
1. **Sign Up / Login:** Register as a "Customer".
2. **Menu Browsing & Cart:**
   - Browse pre-seeded food items (Burger, Pizza, Pasta, Beverage) with price, description, and quantity controls (`+` / `-`).
   - Floating Cart summary displaying total items, total price, delivery address, and a "Place Order" button.
3. **Order Placement:**
   - Writes order to `/orders/{orderId}` with status `PLACED`.
   - Navigates to the Tracking screen or Order History list.
4. **Order History & Active Tracking:**
   - Lists past and active orders.
   - Live Tracking Screen (`OrderTrackingActivity`):
     - Displays visual status stepper: `Placed` ➔ `Accepted` ➔ `Picked Up` ➔ `Out for Delivery` ➔ `Delivered`.
     - Displays `SupportMapFragment` with markers:
       - 🏪 Restaurant location.
       - 🏠 Customer delivery address.
       - 🛵 Driver marker smoothly animating as new GPS updates arrive from `/orders/{orderId}/driverLocation`.
     - Order details card showing ordered items, total, and driver contact info.

### 2.2 Delivery Driver Workflow
1. **Sign Up / Login:** Register as a "Delivery Driver".
2. **Driver Dashboard (`DriverDashboardActivity`):**
   - **Available Orders Tab:** Shows active orders placed by customers (`status == PLACED`).
   - Tapping an order allows the driver to **"Accept Order"** (assigns driver's UID and name, transitions status to `ACCEPTED`).
   - **My Active Delivery Tab:** Quick access to the currently accepted order.
3. **Delivery Execution (`DriverDeliveryActivity`):**
   - Displays customer address and ordered items.
   - Action buttons with clear status advancement:
     - **"Picked Up Order"** (status ➔ `PICKED_UP`)
     - **"Start Delivery"** (status ➔ `OUT_FOR_DELIVERY` and starts `DriverLocationService`)
     - **"Complete Delivery"** (status ➔ `DELIVERED`, stops `DriverLocationService`)
   - Also includes a simulated step button for testing indoors/emulator without moving physically.

---

## 3. Database Schema (Firebase Realtime Database)

### `/users/{uid}`
- `uid`: String (Firebase Auth UID)
- `name`: String
- `email`: String
- `role`: String (`CUSTOMER` | `DRIVER`)
- `createdAt`: Long

### `/orders/{orderId}`
- `orderId`: String
- `customerId`: String
- `customerName`: String
- `customerAddress`: String
- `customerLat`: Double
- `customerLng`: Double
- `restaurantName`: String
- `restaurantAddress`: String
- `restaurantLat`: Double
- `restaurantLng`: Double
- `driverId`: String (null when unassigned)
- `driverName`: String (null when unassigned)
- `status`: String (`PLACED`, `ACCEPTED`, `PICKED_UP`, `OUT_FOR_DELIVERY`, `DELIVERED`)
- `items`: List of `{ name, quantity, price }`
- `totalAmount`: Double
- `createdAt`: Long
- `driverLocation`:
  - `latitude`: Double
  - `longitude`: Double
  - `updatedAt`: Long

---

## 4. Technical Architecture & File Structure

```
app/src/main/java/com/example/fooddeliverytracking/
├── models/
│   ├── User.java
│   ├── MenuItem.java
│   ├── OrderItem.java
│   ├── Order.java
│   └── DriverLocation.java
├── adapters/
│   ├── MenuAdapter.java
│   └── OrderAdapter.java
├── auth/
│   ├── LoginActivity.java
│   └── RegisterActivity.java
├── customer/
│   ├── CustomerHomeActivity.java
│   ├── CustomerOrdersActivity.java
│   └── OrderTrackingActivity.java
├── driver/
│   ├── DriverDashboardActivity.java
│   └── DriverDeliveryActivity.java
├── services/
│   └── DriverLocationService.java
└── utils/
    ├── FirebaseHelper.java
    └── Constants.java
```

### Layout XML Files (`res/layout/`)
- `activity_login.xml`
- `activity_register.xml`
- `activity_customer_home.xml`
- `item_menu.xml`
- `activity_customer_orders.xml`
- `item_order.xml`
- `activity_order_tracking.xml`
- `activity_driver_dashboard.xml`
- `activity_driver_delivery.xml`

---

## 5. Location Service & Map Integration Details
- **FusedLocationProviderClient:** Configured with `Priority.PRIORITY_HIGH_ACCURACY`, interval 5000ms, fastest interval 3000ms.
- **Service:** Started with explicit Intent passing `orderId`.
- **Permissions:** Handled cleanly with runtime permission checks (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS`).
- **Google Maps:** Uses standard `SupportMapFragment`, auto-centers with `LatLngBounds` to display restaurant, customer, and driver in one comfortable view.

---

## 6. Verification & Demonstration Plan
- **Build Verification:** Execute `./gradlew assembleDebug` to ensure all Java classes and XML layouts compile without errors.
- **Authentication Check:** Register Customer account & Driver account. Verify role routing.
- **Order Placement:** Place an order from Customer view and verify write in Firebase `/orders`.
- **Driver Order Pool:** View order in Driver's available queue, accept it, update status through lifecycle.
- **Real-time Map Synchronization:** Verify customer's `OrderTrackingActivity` receives status badges and live marker updates.
