# DriveMarket 🚗

DriveMarket is a modern, high-end automotive marketplace mobile application built for **Android (Kotlin, Material 3)** and powered by **Google Firebase** (Cloud Firestore, Firebase Storage, and Firebase Authentication).

---

## 🌟 Key Features

* **Cloud-Native Real-Time Architecture**: Real-time inventory sync and chat messaging via Google Cloud Firestore snapshot listeners (`addSnapshotListener`).
* **Direct Cloud Storage Media Uploads**: High-resolution vehicle photo uploads directly to Firebase Storage with byte-level progress reporting and fallback caching.
* **Seamless Authentication & Guest Profiles**: Anonymous Firebase Authentication with local & remote display name and phone profile sync.
* **Explore & Browse Feed**: Instant multi-parameter filtering:
  - Search query matching title, make, model, description, and specs.
  - Location filter (Austin, Miami, Seattle, Dallas, etc.).
  - Category & body style chips (All, SUV, Sedan, Electric, Truck, Luxury, Hybrid, Coupe, Under $30k, Low Miles).
  - Multi-option sorting (Recommended, Price: Low to High, Price: High to Low, Newest Year, Lowest Mileage).
* **Rich Vehicle Details**:
  - Full-width hero gallery slider with photo counter.
  - 2x2 vehicle specifications matrix (Year, Mileage, Transmission, Body Style).
  - Interactive financing & loan calculator pill ($/mo estimate, customizable terms, down payment, APR breakdown).
  - Verified seller card with one-tap phone dialing and in-app chat.
* **Live Chat & Negotiation Drawer**:
  - Real-time buyer/seller message threads.
  - Official purchase offer cards with escrow trust guarantee.
  - Role-based interaction: Sellers can Accept or Decline offers; Buyers see real-time review status.
  - Automated difference calculation (e.g. `-$3,000 below asking` or `+$2,000 above asking`).
* **Offline-First Persistence**: Native Cloud Firestore offline disk cache enables smooth offline browsing and instant startup without network delays.

---

## 📂 Project Architecture

```
├── app/                                    # Android Application (Kotlin, Material 3)
│   ├── build.gradle.kts                    # Firebase BoM, Coil, Coroutines, JUnit
│   ├── google-services.json                # Firebase project configuration
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml         # Permissions & Activity declarations
│       │   ├── java/com/automarket/app/
│       │   │   ├── AutoMarketApplication.kt # Firebase & Firestore offline initialization
│       │   │   ├── data/
│       │   │   │   ├── model/              # Models (Car, ChatMessage, Offer, CarFilter)
│       │   │   │   └── repository/         # CarRepository (Firestore & Storage sync)
│       │   │   ├── ui/                     # Activities, Fragments, Adapters
│       │   │   │   ├── adapter/            # CarAdapter with 3-chip matrix & badges
│       │   │   │   ├── chat/               # ChatOffersActivity
│       │   │   │   ├── detail/             # CarDetailActivity & Financing Calculator
│       │   │   │   ├── main/               # MainActivity, Home, Favorites, MyListings
│       │   │   │   └── post/               # PostCarActivity with multi-photo upload
│       │   │   └── util/                   # ImageUtils (Coil extensions)
│       │   └── res/                        # Material 3 layouts, drawables, color tokens
│       └── test/                           # Unit test suite (CarModelTest.kt)
├── build.gradle.kts                        # Root Gradle build script with Google Services plugin
├── gradle.properties                       # Build tuning & Windows path properties
└── README.md                               # Project documentation
```

---

## 🔨 CLI Build & Test Commands

The application builds cleanly via command line without requiring Android Studio:

```bash
# Run unit tests directly via JUnitCore
./gradlew runUnitTests

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

Generated APK artifacts:
* **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
* **Release APK**: `app/build/outputs/apk/release/app-release.apk`
