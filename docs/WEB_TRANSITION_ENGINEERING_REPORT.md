# PVCA — Web-First Transition Engineering Report
## Browser-Based Visual Commerce & Distributed Client-Server Demonstration

**Project:** PVCA (PayPal Visual Commerce Assistant)  
**Package:** `frontend/web` (React + TypeScript + Vite) & `backend` (Python FastAPI)  
**Date:** November 9, 2026  
**Document Status:** Complete & Verified via Production Builds and Test Suites  
**Target Platform:** Primary Web Demonstration (Google Chrome / Desktop Browser) + Secondary Mobile Prototype (Android)  

---

## 1. Executive Summary & Architectural Motivation

To provide an immediate, accessible, and high-impact demonstration for hackathon judges and evaluators, the primary PVCA demonstration interface has transitioned to a **web-first architecture**. 

Instead of requiring an Android emulator, USB debugging, or a physical smartphone, evaluators can launch and interact with PVCA directly on any modern web browser (such as Google Chrome) on Windows, macOS, or Linux, while preserving the existing Android Kotlin/Compose implementation as a complementary mobile client.

### Key Achievements:

1. **Modern Navy/Cyan Responsive Web Frontend (`frontend/web`):**
   * Built with React 18, TypeScript, Vite, and Tailwind CSS.
   * Brand styling: Deep navy (`#0B132B`, `#1C2541`), visual commerce cyan accents (`#48CAE4`), and PayPal-inspired blue (`#0077B6`).
   * Clean brand header with PVCA logo, tagline (*"See it. Identify it. Shop it."*), and real-time backend connection status indicator.

2. **Client-Side CPU-Optimized Object Detection (TensorFlow.js + COCO-SSD):**
   * Migrated object localization from Android ML Kit to client-side **TensorFlow.js** with the **COCO-SSD** model (`lite_mobilenet_v2` backbone).
   * Runs locally in the browser utilizing WebGL with automated CPU fallback; no external inference APIs or server GPU required.
   * Extracts real bounding boxes and probabilities across 80 common object classes.
   * Maps detection classes into PVCA commercial categories (e.g., `backpack`/`handbag` -> `Fashion good`, `bottle`/`cup` -> `Drinkware`, `cell phone`/`laptop`/`remote` -> `Consumer Electronics`, `chair`/`vase` -> `Home good`).

3. **Interactive Visual Commerce Overlay:**
   * **Letterbox-Aware Dynamic Scaling:** Computes exact scale factors between intrinsic image resolution and client display container, ensuring millimeter-accurate bounding boxes across all viewport sizes.
   * **Glowing, Numbered Blue Dots:** Centered on detected objects (`#1`, `#2`, `#3`...) with pulsing cyan glow shadows.
   * **Clickable Object Selection:** Tapping or clicking any dot or pill activates that object and highlights it with a solid 2.5px cyan border.

4. **Integration with Existing FastAPI Backend (`http://127.0.0.1:8000`):**
   * Reuses the Day 4 FastAPI backend unchanged:
     * `GET /api/health` — live healthcheck ping.
     * `POST /api/products/match` — weighted scoring (crop labels, category alignment, scene context, user tokens) and honest confidence classification (`HIGH_CONFIDENCE`, `POSSIBLE_MATCH`, `NO_RELIABLE_MATCH`).
   * Vite dev server configured with automated API proxy (`/api` -> `http://127.0.0.1:8000`).

5. **Single-Best Match Card & Safety Boundaries:**
   * Shows demonstrated price, merchant identity, product ID, and natural language match evidence.
   * **Provenance Disclaimer:** Fictional demonstration products are clearly tagged (`Internal PVCA Verified Demo Catalogue — Demo Data`).
   * **Disabled Store Links:** Store buttons remain strictly disabled (`Store Link Disabled (Demo Catalogue Product)`) to protect users from invalid demo links.
   * **User-Assisted Search Refinement:** Inline query refinement field allows users to provide clarifying text keywords (e.g. *"running shoe"*, *"ceramic mug"*), triggering server-side candidate re-ranking.

6. **Quick-Test Demonstration Presets:**
   * Included curated one-click sample presets (Running Shoes, Ceramic Mug, Leather Backpack, Minimalist Watch) so reviewers can test the pipeline instantly without uploading local files.

---

## 2. Directory Structure & Component Organization

```text
D:\PVCA\
├── backend/                             # Python FastAPI Backend (Preserved)
│   ├── main.py                          # FastAPI endpoints & CORS
│   ├── models.py                        # Pydantic v2 schemas
│   ├── catalogue.py                     # 30-item demo catalogue reader
│   ├── matching.py                      # Weighted ranking algorithm
│   └── tests/test_api.py                # 13 automated backend tests
│
├── frontend/
│   ├── web/                             # Primary Web Demonstration (New)
│   │   ├── index.html                   # HTML entry point with Inter font
│   │   ├── vite.config.ts               # Vite bundler & backend /api proxy
│   │   ├── package.json                 # React, TypeScript, TF.js, Tailwind v4
│   │   └── src/
│   │       ├── types/index.ts           # Shared TypeScript domain contracts
│   │       ├── services/
│   │       │   ├── api.ts               # FastAPI backend client
│   │       │   └── detector.ts          # TensorFlow.js COCO-SSD detector & category mapping
│   │       ├── components/
│   │       │   ├── Header.tsx           # Brand header with backend status indicator
│   │       │   ├── UploadZone.tsx       # Drag-and-drop uploader & 4 quick-try presets
│   │       │   ├── ImageCanvas.tsx      # Aspect-ratio-preserving canvas with numbered blue dots
│   │       │   └── ProductMatchCard.tsx # Best match card, demo warning & refinement form
│   │       ├── App.tsx                  # Master application orchestrator
│   │       ├── main.tsx                 # React DOM mount point
│   │       └── index.css                # Tailwind CSS imports & theme
│   │
│   └── android/                         # Secondary Mobile Prototype (Preserved)
│       ├── app/                         # Jetpack Compose app module
│       └── ...                          # Kotlin source and ML Kit detectors
```

---

## 3. Verification & Test Outcomes

### Web Production Build (`tsc && vite build`):
* Command: `npm run build`
* Modules transformed: 2,852 modules.
* Generated bundles:
  * `dist/index.html` (0.83 kB)
  * `dist/assets/index-*.css` (37.95 kB)
  * `dist/assets/index-*.js` (2,037 kB / 351 kB gzip)
* Result: **SUCCESS** (`built in 19.04s`). Zero TypeScript or bundling errors.

### Backend Automated Test Suite (`pytest`):
* Command: `D:\PVCA\backend\.venv\Scripts\python.exe -m pytest tests\test_api.py -v`
* Result: **13 / 13 PASSED** in 1.90s. Zero regressions.

### Android Unit Tests (`:app:testDebugUnitTest`):
* Command: `gradle.bat :app:testDebugUnitTest`
* Result: **20 / 20 PASSED** in 13s. Android codebase remains 100% intact.

---

## 4. Beginner-Friendly Running Guide (Windows Laptop)

To run the complete PVCA system locally, open **two separate PowerShell terminals**:

### Terminal 1 — Start the FastAPI Backend
```powershell
cd D:\PVCA\backend
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```
*Verification:* Open `http://127.0.0.1:8000/api/health` in your browser. You should see `{"status":"ok","version":"1.0.0"}`.

### Terminal 2 — Start the React Web Demonstration
```powershell
$env:PATH = "D:\PVCA\.tools\node-v20.18.0-win-x64;$env:PATH"
$env:NODE_EXTRA_CA_CERTS = "D:\PVCA\.tools\avast_root.pem"
cd D:\PVCA\frontend\web
npm run dev
```
*Verification:* Open `http://localhost:5173` in Google Chrome.

---

## 5. Security & TLS Configuration Notes

* **Enterprise / Antivirus SSL Root Certificate (`avast_root.pem`):**
  When developing on machines with antivirus or proxy web shields (such as Avast Web Shield), local traffic is signed using a local root CA. In strict compliance with security requirements, SSL verification was **never disabled** (`strict-ssl = true`). Instead, the legitimate root certificate was exported from the Windows Certificate Store into `.tools/avast_root.pem` and passed via `NODE_EXTRA_CA_CERTS`.
* **Zero Hardcoded Secrets:** No live PayPal client secrets or private keys are stored on the frontend.
* **Store Link Safety:** All external product links for demonstration items remain strictly disabled.

---

## 6. Known Limitations & Next Steps

* **COCO-SSD Class Coverage:** COCO-SSD recognizes 80 common object classes. Specific micro-categories (like exact shoe models or obscure electronics) are classified broadly and rely on backend keyword matching or user refinement for exact identification.
* **First Model Download:** On the first image upload, TensorFlow.js downloads the lightweight (~5 MB) MobileNet weights from the official CDN and caches them in the browser for instant subsequent inferences.
* **Looking Ahead to Day 5:**
  * PayPal Sandbox Orders API integration.
  * Server-side order creation (`POST /api/orders/create`) and capture (`POST /api/orders/{id}/capture`).
  * Web PayPal checkout button approval flow.
