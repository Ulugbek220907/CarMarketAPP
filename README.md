# DriveMarket 🚗

DriveMarket is a modern, full-stack automotive marketplace mobile application built with **Android (Kotlin, Material 3)** and powered by a cloud-native **Node.js + PostgreSQL/SQLite** backend ready for **Render** deployment.

---

## 🌟 Key Features

* **Explore & Browse Feed**: Search with real-time debounce, Austin location selector, category chips (SUV, Sedan, Electric, Hybrid, Truck, Luxury), and budget filters (Under $15k, Under $25k, Low Miles, Certified CARFAX).
* **Rich Vehicle Details**: 16:10 hero image gallery, 360° interactive view simulation, 2x2 Bento spec matrix, CARFAX Clean Title badge, and seller reputation profile.
* **Negotiations & Chat**: Contextual pinned listing card, buyer & seller message bubbles, official offer cards with escrow trust badge, quick suggestion chips, and live backend sync.
* **List Your Car**: 3 photo upload dropzone with preview/remove, VIN autofill simulator, condition selection grid, and price valuation guide.
* **Cloud Source of Truth**: All vehicle listings, chat messages, offers, and favorites are stored and synchronized with the Render cloud database.
* **Offline-First Architecture**: Android client caches listings locally in SQLite, guaranteeing instant UI rendering with zero stutter.

---

## 📂 Project Structure

```
├── app/                                    # Android Application (Kotlin, Material 3)
│   ├── build.gradle.kts                    # AGP 8.5.0, Retrofit, OkHttp, Coroutines
│   └── src/main/
│       ├── AndroidManifest.xml             # Permissions & Activity declarations
│       ├── java/com/automarket/app/
│       │   ├── data/
│       │   │   ├── api/                    # Retrofit service & ApiClient singleton
│       │   │   ├── local/                  # SQLite cache helper (DriveMarket schema)
│       │   │   ├── model/                  # Data classes (Car, ChatMessage, Filter)
│       │   │   └── repository/             # CarRepository syncing with Render API
│       │   └── ui/                         # Activities, Fragments, Adapters
│       └── res/                            # Layouts, vector drawables, M3 colors
├── server/                                 # Cloud Backend for Render
│   ├── package.json                        # Express, pg (PostgreSQL), sqlite3, cors
│   ├── Dockerfile                          # Containerized deployment manifest
│   ├── .env.example                        # Environment variables template
│   ├── src/
│   │   ├── index.js                        # Express server & API routes
│   │   ├── db.js                           # Dual PostgreSQL / SQLite abstraction
│   │   ├── seedData.js                     # Initial DriveMarket vehicles & messages
│   │   └── routes/
│   │       ├── cars.js                     # GET /api/cars, POST /api/cars, favorites
│   │       └── messages.js                 # Chat & offer endpoints
│   └── test/
│       └── test_api.js                     # Local database & seed test runner
├── render.yaml                             # 1-Click Render Blueprint (Service + Postgres)
└── README.md                               # Project documentation
```

---

## 🚀 Deploying to Render (100% Free Tier)

Render limits accounts to **one free PostgreSQL database**. To make deployment 100% free and avoid limit conflicts, DriveMarket includes a built-in SQLite engine with automatic initial seeding.

### Option A: 1-Click Blueprint (Recommended)
1. Go to your [Render Dashboard](https://dashboard.render.com).
2. Click **New +** → **Blueprint**.
3. Select your `CarMarketAPP` repository.
4. Render reads `render.yaml` and deploys **`carmarket-api`** on the **Free Tier**.
5. Once deployed, copy your service URL (e.g. `https://carmarket-api-xxxx.onrender.com`).
6. *Optional*: If you already have a PostgreSQL database on Render, simply set `DATABASE_URL` in the Environment tab of `carmarket-api` and it will automatically connect to PostgreSQL!

### Option B: Manual Web Service
1. Create a **Web Service** on Render pointing to your GitHub repo:
   * **Root Directory**: `server`
   * **Build Command**: `npm install`
   * **Start Command**: `npm start`
   * **Plan**: Free
2. Test deployment at `https://<your-service>.onrender.com/health`.

---

## 📱 Connecting the Android App to Render

1. In the Android app, tap the **Bell icon (Notifications)** in the top bar of the home feed.
2. An alert dialog will prompt: **Render Cloud Backend URL**.
3. Paste your live Render URL (e.g., `https://carmarket-api.onrender.com/`) and tap **Save & Connect**.
4. The app will immediately pull all vehicles from your live Render database!

---

## 🔨 CLI Build (No Android Studio Required)

The Android app can be compiled and signed purely via command line:

```bash
# Debug build
./gradlew assembleDebug

# Production Release build (Signed APK)
./gradlew assembleRelease
```

Generated APKs will be located at:
* **Release**: `app/build/outputs/apk/release/app-release.apk`
* **Debug**: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Testing the Backend Locally

```bash
cd server
npm install
node test/test_api.js   # Validates database initialization and seed data
npm start               # Starts local server on http://localhost:10000
```
