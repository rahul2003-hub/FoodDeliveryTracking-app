# Food Delivery Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete, academic-standard Food Delivery Tracking Android application in Java + XML with Firebase Authentication, Realtime Database sync, Google Maps tracking, and Driver GPS broadcasting.

**Architecture:** Unified role-based Android architecture. Authentication routes users to either the Customer Home or Driver Dashboard. Orders placed by customers appear in an open order pool; drivers accept orders and broadcast their location via `DriverLocationService` (Fused Location Provider) while customers track the driver's live location and order status in real time on Google Maps.

**Tech Stack:** Android Java, XML, Firebase Auth, Firebase Realtime Database, Google Play Services (Maps & Location), Material Components.

**Spec:** `docs/superpowers/specs/2026-09-04-food-delivery-tracking-design.md`

## Global Constraints
- Language: Java (Java 11 compatibility)
- UI: Android XML layouts with ViewBinding / findViewById
- Package: `com.example.fooddeliverytracking`
- Compile SDK: 34, Min SDK: 24, Target SDK: 34
- No over-engineering: clean models, adapters, activities, and services

---

### Task 1: Core Models, Constants & Unit Tests

**Files:**
- Create: `app/src/main/java/com/example/fooddeliverytracking/utils/Constants.java`
- Create: `app/src/main/java/com/example/fooddeliverytracking/models/User.java`
- Create: `app/src/main/java/com/example/fooddeliverytracking/models/MenuItem.java`
- Create: `app/src/main/java/com/example/fooddeliverytracking/models/OrderItem.java`
- Create: `app/src/main/java/com/example/fooddeliverytracking/models/DriverLocation.java`
- Create: `app/src/main/java/com/example/fooddeliverytracking/models/Order.java`
- Test: `app/src/test/java/com/example/fooddeliverytracking/ModelUnitTest.java`

**Interfaces:**
- Produces: `Constants.ROLE_CUSTOMER`, `Constants.ROLE_DRIVER`, `Constants.STATUS_*`, POJO models with zero-arg constructors for Firebase Realtime Database deserialization.

- [ ] **Step 1: Write Unit Test for Models**
Create `app/src/test/java/com/example/fooddeliverytracking/ModelUnitTest.java` testing Order creation, total calculation, and status progression.

- [ ] **Step 2: Run Unit Test to verify failure**
Run `./gradlew testDebugUnitTest --tests com.example.fooddeliverytracking.ModelUnitTest`

- [ ] **Step 3: Implement Constants and Model POJOs**
Implement `Constants.java`, `User.java`, `MenuItem.java`, `OrderItem.java`, `DriverLocation.java`, `Order.java` with Firebase compatible constructors and getters/setters.

- [ ] **Step 4: Run Unit Test to verify pass**
Run `./gradlew testDebugUnitTest --tests com.example.fooddeliverytracking.ModelUnitTest`

---

### Task 2: Resources, Drawables, Styles & AndroidManifest

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Create: Vector drawables for food icons, marker icons, status badges in `app/src/main/res/drawable/`

**Interfaces:**
- Produces: Permissions (`INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`), color palette, vector assets, and declared Activities/Service.

- [ ] **Step 1: Define Colors, Theme, and Strings**
Configure vibrant food delivery color palette (primary orange/red `#FF5722`, secondary dark `#212121`, accent green `#4CAF50`) and string resources.

- [ ] **Step 2: Create Vector Drawables**
Create drawables: `ic_burger.xml`, `ic_pizza.xml`, `ic_pasta.xml`, `ic_drink.xml`, `ic_motorcycle.xml`, `ic_restaurant.xml`, `ic_destination.xml`, and status step icons.

- [ ] **Step 3: Update AndroidManifest.xml**
Add required permissions, register all Activities (`LoginActivity`, `RegisterActivity`, `CustomerHomeActivity`, `CustomerOrdersActivity`, `OrderTrackingActivity`, `DriverDashboardActivity`, `DriverDeliveryActivity`), and register `DriverLocationService` with `android:foregroundServiceType="location"`.

---

### Task 3: Authentication & Role Routing

**Files:**
- Create: `app/src/main/res/layout/activity_login.xml`
- Create: `app/src/main/res/layout/activity_register.xml`
- Create: `app/src/main/java/com/example/fooddeliverytracking/auth/LoginActivity.java`
- Create: `app/src/main/java/com/example/fooddeliverytracking/auth/RegisterActivity.java`
- Modify: `app/src/main/java/com/example/fooddeliverytracking/MainActivity.java`

**Interfaces:**
- Consumes: Firebase Auth, Firebase Database `/users/{uid}`, `Constants.ROLE_*`
- Produces: Seamless session restore, sign-in, registration with role radio selection (`Customer` vs `Driver`), and automatic redirect.

- [ ] **Step 1: Create Login & Registration Layouts**
Design clean, professional Material card layouts with email, password, name, role selection radio group, and loading progress indicators.

- [ ] **Step 2: Implement Registration Logic**
In `RegisterActivity.java`, register with `FirebaseAuth.createUserWithEmailAndPassword`, save `User` object to `/users/{uid}` in Realtime Database, and route to corresponding dashboard.

- [ ] **Step 3: Implement Login Logic & Session Check**
In `LoginActivity.java` and `MainActivity.java`, authenticate with `signInWithEmailAndPassword`, query `/users/{uid}/role`, and route appropriately.

---

### Task 4: Customer Menu & Cart Flow

**Files:**
- Create: `app/src/main/res/layout/item_menu.xml`
- Create: `app/src/main/res/layout/activity_customer_home.xml`
- Create: `app/src/main/java/com/example/fooddeliverytracking/adapters/MenuAdapter.java`
- Create: `app/src/main/java/com/example/fooddeliverytracking/customer/CustomerHomeActivity.java`

**Interfaces:**
- Consumes: `MenuItem`, `OrderItem`, `Order`, Firebase Database `/orders`
- Produces: Interactive food menu, quantity counter (`+`/`-`), live subtotal calculation, floating cart view, and order submission to Firebase.

- [ ] **Step 1: Create Menu Item and Home Layouts**
Build `item_menu.xml` with card design, item image icon, title, description, price, and `+`/`-` quantity controls. Build `activity_customer_home.xml` with RecyclerView, address bar, cart summary bar, and toolbar with "My Orders" button.

- [ ] **Step 2: Implement MenuAdapter**
Implement RecyclerView Adapter managing quantity increments, decrements, and total cart price notifications.

- [ ] **Step 3: Implement CustomerHomeActivity**
Populate menu list, handle address entry, generate unique `orderId`, write complete `Order` object to Firebase Realtime Database under `/orders/{orderId}`, and navigate to `OrderTrackingActivity`.

---

### Task 5: Customer Order History & Live Map Tracking

**Files:**
- Create: `app/src/main/res/layout/item_order.xml`
- Create: `app/src/main/res/layout/activity_customer_orders.xml`
- Create: `app/src/main/res/layout/activity_order_tracking.xml`
- Create: `app/src/main/java/com/example/fooddeliverytracking/adapters/OrderAdapter.java`
- Create: `app/src/main/java/com/example/fooddeliverytracking/customer/CustomerOrdersActivity.java`
- Create: `app/src/main/java/com/example/fooddeliverytracking/customer/OrderTrackingActivity.java`

**Interfaces:**
- Consumes: `SupportMapFragment`, Google Maps API, Firebase `/orders/{orderId}`, `/orders/{orderId}/driverLocation`
- Produces: Real-time map rendering with Restaurant, Customer, and Driver markers, status stepper (`Placed` -> `Accepted` -> `Picked Up` -> `Out for Delivery` -> `Delivered`), and live driver marker animation.

- [ ] **Step 1: Create Order Item and Orders List Layout & Activity**
Build `item_order.xml` with order ID, date, total, status badge, and "Track Order" button. Implement `CustomerOrdersActivity.java` listening to `/orders` filtered by `customerId`.

- [ ] **Step 2: Create Live Order Tracking Layout**
Build `activity_order_tracking.xml` featuring:
1. Status stepper banner (`PLACED` -> `ACCEPTED` -> `PICKED_UP` -> `OUT_FOR_DELIVERY` -> `DELIVERED`).
2. Google Maps `SupportMapFragment`.
3. Bottom sheet/card with driver name, phone, item summary, and total amount.

- [ ] **Step 3: Implement OrderTrackingActivity**
Initialize Google Map, add Restaurant and Customer markers, set camera bounds. Attach `ValueEventListener` to `/orders/{orderId}` to update status indicators and update/animate the Driver's motorcycle marker position smoothly.

---

### Task 6: Driver Dashboard & Delivery Management

**Files:**
- Create: `app/src/main/res/layout/activity_driver_dashboard.xml`
- Create: `app/src/main/res/layout/activity_driver_delivery.xml`
- Create: `app/src/main/java/com/example/fooddeliverytracking/driver/DriverDashboardActivity.java`
- Create: `app/src/main/java/com/example/fooddeliverytracking/driver/DriverDeliveryActivity.java`

**Interfaces:**
- Consumes: Firebase `/orders`, `Constants.STATUS_*`
- Produces: Driver order acceptance (`status = ACCEPTED`, `driverId = uid`), status transitions (`PICKED_UP`, `OUT_FOR_DELIVERY`, `DELIVERED`), and triggers `DriverLocationService`.

- [ ] **Step 1: Create Driver Dashboard Layout & Logic**
Build `activity_driver_dashboard.xml` with two lists/tabs: "Available Orders" (`status == PLACED`) and "My Active Deliveries". Implement `DriverDashboardActivity.java` allowing the driver to tap "Accept Order", updating Firebase `/orders/{orderId}`.

- [ ] **Step 2: Create Driver Delivery Layout**
Build `activity_driver_delivery.xml` with customer destination details, items list, action buttons ("Mark Picked Up", "Start Delivery / Broadcast GPS", "Complete Delivery"), and a "Simulate Driver Step" button for testing on emulators.

- [ ] **Step 3: Implement DriverDeliveryActivity**
Update order status in Firebase on each button tap. Launch `DriverLocationService` when status reaches `OUT_FOR_DELIVERY` and stop the service when `DELIVERED`.

---

### Task 7: Driver Background Location Service

**Files:**
- Create: `app/src/main/java/com/example/fooddeliverytracking/services/DriverLocationService.java`

**Interfaces:**
- Consumes: `FusedLocationProviderClient`, LocationRequest (5s interval), Foreground Service notification channel.
- Produces: Continuous location updates written to `/orders/{orderId}/driverLocation` in Firebase Realtime Database.

- [ ] **Step 1: Implement Foreground Service Lifecycle & Notification**
Create `DriverLocationService.java` with a Foreground Service notification channel ("Driver Live Location Tracking").

- [ ] **Step 2: Implement FusedLocationProviderClient Updates**
Configure `LocationRequest` with `Priority.PRIORITY_HIGH_ACCURACY`, interval 5000ms, fastest interval 3000ms. On `onLocationResult`, write `latitude`, `longitude`, and `updatedAt` to `/orders/{orderId}/driverLocation`.

- [ ] **Step 3: Clean Service Teardown**
Remove location updates in `onDestroy()`, ensuring battery efficiency and preventing memory leaks.

---

### Task 8: Full Build & Integration Verification

- [ ] **Step 1: Compile Application**
Run `./gradlew assembleDebug` to verify all Java source files, XML layouts, and Manifest configurations build cleanly.

- [ ] **Step 2: Run Unit Tests**
Run `./gradlew testDebugUnitTest` to ensure all tests pass.

- [ ] **Step 3: Prepare Project Documentation**
Provide instructions for running the project on an Android device or emulator, logging in as Customer & Driver, and presenting the project.
