# PVCA — PayPal Visual Commerce Assistant

> **See it. Identify it. Shop it.**
> 
> *An AI-powered visual commerce assistant connecting on-screen product discovery with seamless PayPal checkout.*

---

## Project Status

- **Deadline:** November 12, 2026
- **Primary Demonstration:** Interactive Web Application (`frontend/web`)
- **Secondary Prototype:** Android Mobile Client (`frontend/android`)
- **Active Milestones:**
  - [x] **Milestone 0 — Application Foundation:** Android foundation and Gradle Version Catalog.
  - [x] **Milestone 1 — Image Selection & AI Image Labeling:** Photo Picker and Google ML Kit Image Labeling.
  - [x] **Milestone 2 — Object Detection & Interactive Blue Dot:** On-device object localization, letterbox-aware coordinate transformation, and interactive glowing blue dot selection.
  - [x] **Milestone 3 — Visual Identity Polishing:** Refined non-overlapping numbered blue dots and uncluttered bounding box overlays.
  - [x] **Milestone 4 — Verified Product Catalogue & Matching Engine:** 30 curated demonstration products, object-specific crop analysis, weighted ranking, and user-assisted search fallback.
  - [x] **Milestone 6 — Distributed FastAPI Backend:** Client-server architecture with health, catalogue, search, and matching endpoints.
  - [x] **Web-First Demonstration:** React 18, TypeScript, Tailwind CSS, Vite, and TensorFlow.js client-side COCO-SSD object detection.
  - [ ] **Milestone 7 — PayPal Sandbox Integration:** (Upcoming — Day 5)

---

## Repository Structure

```text
PVCA/
├── AGENTS.md                  # Operational rules and role definitions
├── MASTER_HANDOFF.md          # Canonical project vision and architectural decisions
├── backend/                   # Python FastAPI service (Catalogue, Search, Matching)
│   ├── main.py                # FastAPI routes & CORS middleware
│   ├── models.py              # Pydantic v2 schemas
│   ├── catalogue.py           # 30-item demonstration product catalogue
│   ├── matching.py            # Transparent weighted ranking engine
│   └── tests/test_api.py      # Automated backend test suite (13 tests)
├── frontend/
│   ├── web/                   # Primary Hackathon Demonstration (React + TS + Vite + TF.js)
│   │   ├── src/               # React application, components, and TF.js detector
│   │   └── package.json       # Web dependencies and scripts
│   └── android/               # Secondary Mobile Prototype (Kotlin + Jetpack Compose + ML Kit)
│       └── app/               # Android mobile application module
└── docs/                      # Architectural specifications & daily engineering reports
```

---

## Quick-Start Running Guide (Windows)

### 1. Primary Demonstration — Web Application (Chrome)

Open two PowerShell windows:

#### Terminal 1 — Start the FastAPI Backend
```powershell
cd D:\PVCA\backend
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```
*Healthcheck:* [http://127.0.0.1:8000/api/health](http://127.0.0.1:8000/api/health) -> `{"status":"ok","version":"1.0.0"}`

#### Terminal 2 — Start the React Web Application
```powershell
$env:PATH = "D:\PVCA\.tools\node-v20.18.0-win-x64;$env:PATH"
$env:NODE_EXTRA_CA_CERTS = "D:\PVCA\.tools\avast_root.pem"
cd D:\PVCA\frontend\web
npm run dev
```
Open **[http://localhost:5173](http://localhost:5173)** in Google Chrome.

---

### 2. Secondary Prototype — Android Mobile Application

To run the Android client on a physical phone (e.g. Motorola Edge 60 Fusion):

1. Forward backend traffic over USB debugging:
   ```powershell
   adb reverse tcp:8000 tcp:8000
   ```
2. Build and launch debug APK:
   ```powershell
   $env:JAVA_HOME = "C:\Users\RADHAKRISHNAN\.jdks\jbr-21.0.11"
   cd D:\PVCA\frontend\android
   & "C:\Users\RADHAKRISHNAN\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\bin\gradle.bat" :app:installDebug
   ```

---

## Canonical Documentation

- [MASTER_HANDOFF.md](file:///D:/PVCA/MASTER_HANDOFF.md): Master specifications and architectural requirements.
- [AGENTS.md](file:///D:/PVCA/AGENTS.md): Coding rules, security boundaries, and instructions.
- [WEB_TRANSITION_ENGINEERING_REPORT.md](file:///D:/PVCA/docs/WEB_TRANSITION_ENGINEERING_REPORT.md): Web architecture, browser detection, and test reports.
- [DAY4_ENGINEERING_REPORT.md](file:///D:/PVCA/docs/DAY4_ENGINEERING_REPORT.md): FastAPI backend and Android hybrid integration report.
- [DAY3_ENGINEERING_REPORT.md](file:///D:/PVCA/docs/DAY3_ENGINEERING_REPORT.md): On-device matching engine and mobile UI report.
