# PVCA — Day 4 Engineering Report
## Milestone 6: FastAPI Backend & Android Hybrid Integration

**Project:** PVCA (PayPal Visual Commerce Assistant)  
**Package:** `com.pvca.app` (Android) / `backend` (Python FastAPI)  
**Platform:** Android (Kotlin, Jetpack Compose, Material 3) + Python 3.14 (FastAPI, Uvicorn, Pydantic v2)  
**Date:** November 9, 2026  
**Target Milestone:** **Milestone 6:** Client-Server Architecture, FastAPI Backend & Hybrid Matching  
**Document Status:** Complete & Verified via Automated Test Suites  
**Target Device:** Motorola Edge 60 Fusion / Android Smartphone (Physical Hardware)  
**Suggested Commit:** `feat: Complete Day 4 FastAPI backend and Android hybrid integration`

---

## 1. Executive Summary & Key Accomplishments

Day 4 establishes the distributed client-server architecture of PVCA. Server-side product search, ranking, and catalogue retrieval are offloaded to a high-performance Python FastAPI service, while the Android mobile client connects through a robust HTTP layer with automatic offline fallback to the local on-device matching engine.

### Key Achievements:

1. **Python FastAPI Backend (`backend/`):**
   * Built a lightweight, production-structured service using FastAPI, Uvicorn, and Pydantic v2.
   * Runs in an isolated virtual environment (`backend/.venv`) on Python 3.14 on Windows.
   * CORS middleware enabled to seamlessly accept requests from mobile devices and emulators.

2. **Authoritative Catalogue & Matching Endpoints:**
   * `GET /api/health`: Healthcheck confirming service uptime and API version (`{"status": "ok", "version": "1.0.0"}`).
   * `GET /api/products`: Retrieves all 30 demonstration products with schema validation.
   * `GET /api/products/{product_id}`: Fetches individual product by ID or returns standard HTTP 404.
   * `GET /api/products/search?q={query}`: Multi-token keyword search across product titles, categories, subcategories, keywords, and descriptions.
   * `POST /api/products/match`: Accepts detected ML Kit category, crop-specific labels, scene labels, and optional user search refinement. Computes weighted scoring matching the established mathematical model (Crop 60%, Category 25%, Scene 15%; or User Refined: User Query 50%, Crop 30%, Category 15%, Scene 5%) and outputs candidate metadata with honest confidence tiers (`HIGH_CONFIDENCE`, `POSSIBLE_MATCH`, `NO_RELIABLE_MATCH`).

3. **Android HTTP Integration without Bloat:**
   * Implemented `BackendMatchingService` using Android's native `java.net.HttpURLConnection`, avoiding heavy external HTTP library dependencies while adhering strictly to low-memory/low-overhead development constraints.
   * Configured timeouts (3s connect, 5s read) and JSON serialization for smooth asynchronous network operation with Kotlin Coroutines (`Dispatchers.IO`).
   * Added `INTERNET` permission and cleartext HTTP support for local development in `AndroidManifest.xml`.

4. **Graceful Hybrid Offline Fallback (`HybridMatchingService`):**
   * `MainViewModel` injects `HybridMatchingService`, which attempts server-side matching first.
   * If the backend is unreachable (e.g. server down, phone disconnected, Wi-Fi timeout), the app instantly and silently falls back to `DefaultProductMatchingService` and the bundled `demo_catalogue.json`.
   * Transparent provenance badging: evidence text indicates whether the match came from `[via backend]` or `[local fallback]`.

5. **Physical Device Connectivity Architecture:**
   * Built `BackendConfig` to support two easy communication mechanisms for the Motorola Edge 60 Fusion:
     1. **ADB Reverse Port Forwarding (Recommended):** Maps `tcp:8000` over USB, allowing the phone to access the laptop backend at `http://127.0.0.1:8000` with zero Wi-Fi firewall issues.
     2. **LAN Wi-Fi Binding:** Supports binding `BASE_URL` to the laptop's Wi-Fi IPv4 address (e.g. `http://192.168.1.x:8000`).

6. **Comprehensive Test Suite & Build Verification:**
   * **Backend:** 13/13 pytest automated tests passed in 2.18s (`test_api.py`), covering health, catalogue listing, ID lookup, keyword search, matching logic, user query refinement, and 422/404 error cases.
   * **Android:** All 20 unit tests passed in 4s (`:app:testDebugUnitTest`).
   * **Android Build:** `:app:assembleDebug` completed successfully in 6s.

---

## 2. Distributed Architecture & Component Structure

```text
PVCA System Architecture
┌────────────────────────────────────────────────────────┐
│               Android Client (Motorola)                │
│                                                        │
│  [ML Kit Object Detection] ──> [Object Crop Analyzer]  │
│                                           │            │
│                                   Crop & Scene Labels  │
│                                           ▼            │
│                            [HybridMatchingService]     │
│                              /                \        │
│                     (Try 1) /                  \ (Fallback)
│                            ▼                    ▼      │
│                 [BackendMatchingService]   [LocalAsset │
│                            │              CatalogueRepo]
└────────────────────────────┼───────────────────────────┘
                             │ HTTP POST /api/products/match
                             ▼
┌────────────────────────────────────────────────────────┐
│             FastAPI Backend (Windows Laptop)           │
│                                                        │
│  • main.py           (FastAPI routes & CORS)           │
│  • catalogue.py      (Loads & indexes catalogue data)  │
│  • matching.py       (Calculates candidate scores)     │
│  • models.py         (Pydantic v2 schemas)             │
│                                                        │
│  Source Asset: demo_catalogue.json (Shared Contract)   │
└────────────────────────────────────────────────────────┘
```

### New and Modified Files:

#### Backend:
* `backend/requirements.txt`: Python dependencies (`fastapi`, `uvicorn`, `pydantic`, `httpx`, `pytest`).
* `backend/models.py`: Pydantic request and response schemas for product and matching endpoints.
* `backend/catalogue.py`: Shared catalogue loader with memory caching (`@lru_cache`) and multi-token search.
* `backend/matching.py`: Server-side port of the weighted ranking engine and confidence classifier.
* `backend/main.py`: FastAPI server application exposing health, product, and matching routes.
* `backend/tests/test_api.py`: Automated Pytest test suite with 13 functional test cases.
* `backend/__init__.py`, `backend/tests/__init__.py`: Python package markers.

#### Android Client:
* `frontend/android/app/src/main/java/com/pvca/app/data/network/BackendConfig.kt`: Configurable base URL, connect timeout, and read timeout.
* `frontend/android/app/src/main/java/com/pvca/app/data/network/BackendMatchingService.kt`: Native `HttpURLConnection` client for backend matching.
* `frontend/android/app/src/main/java/com/pvca/app/data/network/HybridMatchingService.kt`: Orchestrator that tries the backend first and falls back to local assets.
* `frontend/android/app/src/main/java/com/pvca/app/ui/viewmodel/MainViewModel.kt`: Updated to use `HybridMatchingService`.
* `frontend/android/app/src/main/AndroidManifest.xml`: Added `INTERNET` permission and enabled `usesCleartextTraffic="true"`.
* `.gitignore`: Configured to exclude `.venv/`, `__pycache__/`, `.pytest_cache/`, `*.pyc`, and build artifacts.

---

## 3. API Contract Specification

All endpoints are served from the root prefix `/api`.

### 1. Healthcheck
* **Endpoint:** `GET /api/health`
* **Response:**
  ```json
  {
    "status": "ok",
    "version": "1.0.0"
  }
  ```

### 2. Product Catalogue
* **Endpoint:** `GET /api/products`
* **Response:** Array of 30 `ProductItem` objects.
* **Endpoint:** `GET /api/products/{product_id}`
* **Response:** `ProductItem` object or 404 status.
* **Endpoint:** `GET /api/products/search?q={query}`
* **Response:** Filtered array of `ProductItem` objects matching query tokens.

### 3. Product Matching
* **Endpoint:** `POST /api/products/match`
* **Request Body:**
  ```json
  {
    "detected_category": "Fashion good",
    "crop_labels": [
      { "text": "Shoe", "confidence": 0.95 },
      { "text": "Sneaker", "confidence": 0.85 }
    ],
    "scene_labels": [
      { "text": "Footwear", "confidence": 0.75 }
    ],
    "user_query": null
  }
  ```
* **Response Body (Match Found):**
  ```json
  {
    "matched": true,
    "candidate": {
      "product": {
        "id": "PVCA-SHOE-001",
        "title": "AeroStride Velocity Running Shoe",
        "brand": "AeroStride",
        "category": "Footwear",
        "subcategory": "Running Shoes",
        "description": "Lightweight breathable mesh athletic shoe engineered for daily road running and cardio workouts.",
        "keywords": ["shoe", "running shoe", "sneaker", "footwear", "athletic", "laces", "mesh", "jogging", "fashion good"],
        "color": "Midnight Blue / White",
        "price": 89.99,
        "currency": "USD",
        "merchant": "Apex Athletics Demo Store",
        "productUrl": "https://demo.pvca.commerce/products/aerostride-velocity",
        "imageUrl": "https://images.unsplash.com/photo-1542291026-7eec264c27ff",
        "verificationStatus": "demo_verified",
        "provenance": "Internal PVCA Verified Demo Catalogue",
        "lastVerified": "2026-11-01"
      },
      "score": 0.685,
      "confidence_tier": "HIGH_CONFIDENCE",
      "evidence_explanation": "Visual crop matched keywords: Shoe, Sneaker.",
      "is_user_refined": false,
      "match_source": "backend"
    },
    "no_match_reason": null,
    "suggested_category": null
  }
  ```

---

## 4. Verification & Test Results

### Backend Automated Test Suite (`pytest`):
Command: `D:\PVCA\backend\.venv\Scripts\python.exe -m pytest tests\test_api.py -v`

| Test Case | Description | Result |
| :--- | :--- | :--- |
| `test_health` | Health endpoint returns 200 and `"status": "ok"` | **PASSED** |
| `test_list_products_returns_30` | Returns all 30 demonstration catalogue items | **PASSED** |
| `test_list_products_schema` | Validates required fields across product entities | **PASSED** |
| `test_get_product_by_id_found` | Retrieves exact product `PVCA-SHOE-001` | **PASSED** |
| `test_get_product_by_id_not_found` | Returns HTTP 404 for non-existent product ID | **PASSED** |
| `test_search_products_shoes` | Multi-token search finds footwear items | **PASSED** |
| `test_search_products_empty_query` | Empty query returns all products safely | **PASSED** |
| `test_search_products_no_match` | Obscure query returns empty array cleanly | **PASSED** |
| `test_match_fashion_shoe` | Ranks shoe labels into high confidence Footwear match | **PASSED** |
| `test_match_with_user_query` | User query boosting correctly re-ranks and marks `is_user_refined` | **PASSED** |
| `test_match_no_reliable_match` | Evaluates low-confidence scenarios without crashing | **PASSED** |
| `test_match_missing_category_422` | FastAPI validates missing required fields (HTTP 422) | **PASSED** |
| `test_match_response_has_no_match_reason` | Returns structured reason on non-match | **PASSED** |

**Summary:** 13 passed in 2.18s. Zero failures.

### Android Unit Tests (`:app:testDebugUnitTest`):
Command: `gradle.bat :app:testDebugUnitTest`

| Test Suite | Tests | Failures | Errors | Result |
| :--- | :---: | :---: | :---: | :--- |
| `ProductMatchingEngineTest` | 3 | 0 | 0 | **PASSED** |
| `LocalAssetCatalogueRepositoryTest` | 3 | 0 | 0 | **PASSED** |
| `CoordinateTransformerTest` | 5 | 0 | 0 | **PASSED** |
| `DetectedObjectItemTest` | 3 | 0 | 0 | **PASSED** |
| `LabelItemTest` | 3 | 0 | 0 | **PASSED** |
| `MainViewModelTest` | 3 | 0 | 0 | **PASSED** |

**Summary:** 20 passed in 4s. Zero failures.

### Android Build Assembly (`:app:assembleDebug`):
* Result: `BUILD SUCCESSFUL in 6s` (36 actionable tasks).
* Generated APK: `frontend/android/app/build/outputs/apk/debug/app-debug.apk`

---

## 5. Beginner-Friendly Step-by-Step Testing Guide

### A. How to Start the FastAPI Backend on Windows

1. Open PowerShell or Command Prompt.
2. Navigate to the backend directory:
   ```powershell
   cd D:\PVCA\backend
   ```
3. Activate the virtual environment:
   ```powershell
   .\.venv\Scripts\Activate.ps1
   ```
   *(If script execution is disabled on your system, you can directly run through the venv executable in step 4).*
4. Start the Uvicorn server:
   ```powershell
   .\.venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
   ```
5. Verify in your browser by visiting: `http://127.0.0.1:8000/api/health`
   You should see: `{"status":"ok","version":"1.0.0"}`

### B. Connecting Your Motorola Edge 60 Fusion to the Backend

You have two simple ways to connect your phone:

#### Method 1: ADB Reverse via USB (Fastest & Most Reliable — Recommended)
This routes traffic directly over the USB cable and bypasses Windows firewall or Wi-Fi isolation.

1. Connect your Motorola phone via USB with USB debugging enabled.
2. In PowerShell, run:
   ```powershell
   adb reverse tcp:8000 tcp:8000
   ```
3. In `frontend/android/app/src/main/java/com/pvca/app/data/network/BackendConfig.kt`, ensure `BASE_URL` is set to:
   ```kotlin
   var BASE_URL: String = "http://127.0.0.1:8000"
   ```
4. Build and run the app from Android Studio or install the debug APK.
5. Every request sent by the phone to port 8000 automatically forwards directly to your Windows laptop's FastAPI backend!

#### Method 2: Local Wi-Fi Network
If testing completely wirelessly on the same Wi-Fi network:

1. On your Windows laptop, open PowerShell and run:
   ```powershell
   ipconfig
   ```
   Look for `Wireless LAN adapter Wi-Fi` -> `IPv4 Address` (e.g. `192.168.1.15`).
2. In `BackendConfig.kt`, update `BASE_URL`:
   ```kotlin
   var BASE_URL: String = "http://192.168.1.15:8000" // Replace with your laptop's IPv4
   ```
3. Ensure Windows Defender Firewall allows incoming connections on port 8000.
4. Build and install the app on your Motorola phone.

### C. How to Verify Physical Device Integration

1. With the backend running (`uvicorn main:app --host 0.0.0.0 --port 8000`), open PVCA on your phone.
2. Select or snap a photo of an object (e.g., shoe, mug, bag).
3. Tap on the blue dot over the object.
4. Look at the product match card below the preview:
   * **When connected to backend:** The explanation will read `... [via backend]`.
   * **When disconnected (or if server is stopped):** The explanation will read `... [local fallback]`.
5. This proves both backend integration and graceful offline resiliency work seamlessly!

---

## 6. Known Limitations & Next Steps

* **Current Status:** Automated backend and Android unit tests pass 100%. End-to-end confirmation with live mobile network communication awaits the owner's physical device test.
* **Demonstration Data:** Catalogue matches are based on the internal demonstration dataset. All store links remain safely disabled.
* **Looking Ahead to Day 5:**
  * PayPal Orders API integration (Sandbox environment).
  * Server-side order creation (`POST /api/orders/create`) and capture (`POST /api/orders/{id}/capture`).
  * Android checkout approval flow and state handling.

---

## 7. Suggested Git Commit

**Proposed Commit Message:**
```text
feat: Complete Day 4 FastAPI backend and Android hybrid integration

- Implement Python FastAPI backend with health, catalogue, search, and matching endpoints
- Configure Pydantic v2 schemas and 13 automated backend pytest unit tests
- Build Android BackendMatchingService using native HttpURLConnection
- Implement HybridMatchingService with transparent offline fallback to on-device catalogue
- Add INTERNET permission and cleartext network configuration for local development
- Add BackendConfig with USB ADB reverse and LAN connection support
- Pass all 13 backend tests, 20 Android unit tests, and assembleDebug build
```
