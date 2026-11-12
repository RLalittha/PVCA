# PVCA — PayPal Visual Commerce Assistant

**See it. Identify it. Shop it.**

PVCA is an AI-powered Android visual shopping prototype that helps users discover products shown on their phone screen, identify them using Google Gemini, find suitable items in a demonstration catalogue, and continue to PayPal Sandbox checkout.

## Project Overview

PVCA was developed as a hackathon prototype to make visual product discovery more convenient.

The **native Android application is the primary demonstration**. An additional browser-based prototype is included in the repository.

### Working Android Demo

The following workflow has been tested on a Motorola Edge 60 Fusion:

1. Launch PVCA and approve the required Android permissions.
2. Open a product image on the phone.
3. Use the floating **Scan** control.
4. Tap a detected object's blue marker.
5. PVCA analyzes the selected product using Google Gemini.
6. The backend searches the local demonstration catalogue.
7. A matching product card appears when a suitable item is found.
8. Open PayPal Sandbox checkout from the product card.

**Verified example:** Black over-ear headphones were identified and matched to *SoundWave Pro Hybrid Active Noise-Cancelling Headphones*. PayPal Sandbox checkout opened in the browser, with PVCA's floating overlays removed.

The payment integration uses **PayPal Sandbox**. No real purchase is required.

## Key Features

- Android screen capture using MediaProjection
- Floating Scan control
- Google ML Kit object detection
- Interactive blue-dot object selection
- Google Gemini visual product identification
- FastAPI backend
- Local demonstration product catalogue
- Custom product matching and ranking
- Manual search refinement
- Product-type compatibility filtering
- Safe no-match results for unsupported products
- PayPal Sandbox checkout redirect
- Additional React web prototype

## Technology Stack

| Component | Technology |
| --- | --- |
| Android app | Kotlin, Jetpack Compose |
| Screen capture | Android MediaProjection |
| Object detection | Google ML Kit |
| AI identification | Google Gemini API |
| Backend | Python, FastAPI |
| Product matching | Custom catalogue ranking |
| Payment demonstration | PayPal Sandbox REST API |
| Additional web prototype | React, TypeScript, Vite |
| Development assistance | Antigravity AI coding assistant |

## Repository Structure

- `backend/` — FastAPI backend, AI integration, matching logic, tests
- `frontend/android/` — Native Android application
- `frontend/web/` — Additional web prototype
- `start_pvca.bat` — Windows backend launcher

## Setup

### Backend

You will need Python, internet connectivity, a Gemini API key, and PayPal Sandbox credentials.

1. Open a terminal in the `backend` directory.
2. Create and activate a Python virtual environment.
3. Install the backend dependencies using the dependency file provided in the repository.
4. Copy `backend/.env.example` to `backend/.env`.
5. Enter your own credentials in `.env`.
6. Start the FastAPI application using the provided Windows launcher or an appropriate Uvicorn command.

**Security:** Never commit `.env`, API keys, PayPal secrets, or local machine configuration files.

### Android Application

1. Open `frontend/android` in Android Studio.
2. Sync the Gradle project.
3. Connect an Android device with USB debugging enabled.
4. Start the backend.
5. Forward the backend port to the Android device using:

```powershell
adb reverse tcp:8000 tcp:8000
```

6. Build and run the Android application.
7. Grant the requested overlay and screen-capture permissions.
8. Open a product image and use the floating Scan control.

## Testing

Backend tests have been run successfully.

**Latest verified backend test result:** 28 passed, 2 skipped, 1 deselected.

Android unit tests have also completed successfully using the Gradle task `:app:testDebugUnitTest`.

## Demonstrated Results

**Headphones:** Successfully identified and matched to a headphones product in the demonstration catalogue. PayPal Sandbox checkout opened successfully.

**USB cable:** Returned a no-match result rather than recommending an unrelated product.

## Known Limitations

- PVCA uses a limited local demonstration catalogue, not live listings from every retailer.
- Gemini visual identification may occasionally be inaccurate.
- Catalogue matches are recommendations, not guaranteed exact-product identification.
- Unsupported products may produce a no-match result.
- Gemini and PayPal Sandbox features require internet connectivity.
- Android screen-capture restrictions may prevent analysis of protected content.
- This is a hackathon prototype, not a production commerce or payment application.

## Demo Video

YouTube demo: **Link to be added before submission.**

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).

Copyright © 2026 RLalittha.