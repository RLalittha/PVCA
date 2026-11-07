# PVCA Android Application

**Project:** PVCA (PayPal Visual Commerce Assistant)  
**Package:** `com.pvca.app`  
**Platform:** Android (Kotlin, Jetpack Compose, Material 3)  
**Current Milestone:** Milestone 2 (Real Object Detection, Bounding Boxes & Interactive Blue Dot)

---

## Architecture Overview (Milestone 2)

```text
com.pvca.app
├── MainActivity.kt                       # Application entry point (Edge-to-Edge ComponentActivity)
├── data/
│   ├── model/
│   │   ├── DetectedObjectItem.kt        # Localized object model with bounding box & category
│   │   ├── LabelItem.kt                 # Image-level label domain model with confidence %
│   │   ├── AnalysisOutput.kt            # Aggregated output of dual visual analysis
│   │   └── UiState.kt                   # Sealed interface: Idle, Loading, Success, Empty, Error
│   └── analyzer/
│       ├── ImageAnalyzer.kt             # Interface contract for image-level labeling
│       ├── MlKitImageAnalyzer.kt        # ML Kit Image Labeling (On-Device, CPU-friendly)
│       ├── MlKitObjectDetector.kt       # ML Kit Object Detection (Bounding boxes & categories)
│       └── CombinedVisualAnalyzer.kt    # Concurrent coroutine coordinator for dual analysis
└── ui/
    ├── PvcaApp.kt                       # Compose screen coordinating picker, overlay & results
    ├── components/
    │   ├── PvcaHeader.kt                # Brand header ("PVCA: See it. Identify it. Shop it.")
    │   ├── ImagePreviewSection.kt       # Image container with detection overlay & selected card
    │   ├── DetectionBoundingBoxOverlay.kt # Bounding box renderer & interactive pulsing blue dots
    │   ├── SelectedObjectCard.kt        # Info card displaying details for the active object
    │   └── AnalysisResultsSection.kt    # Dual-tab layout (Detected Objects & Image Labels)
    ├── util/
    │   └── CoordinateTransformer.kt     # Exact aspect-ratio aware coordinate mapping (ContentScale.Fit)
    ├── viewmodel/
    │   └── MainViewModel.kt             # StateFlow management with object selection support
    └── theme/
        ├── Color.kt                     # PayPal blue (#0070BA) & cyan accent palette
        ├── Type.kt                      # Clean typography definitions
        └── Theme.kt                     # Material 3 light/dark support
```

---

## Key Milestone 2 Capabilities

1. **Real Object Detection:**
   * Uses Google ML Kit Object Detection & Tracking in static image mode (`com.google.mlkit:object-detection:17.0.2`).
   * Bundled on-device model running locally on CPU.
   * Extracts spatial bounding boxes in upright image pixel coordinates.

2. **Accurate Coordinate Transformation:**
   * [`CoordinateTransformer.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/ui/util/CoordinateTransformer.kt) maps image pixel coordinates to screen display coordinates.
   * Correctly accounts for aspect ratio, letterboxing, and pillarboxing when rendered with `ContentScale.Fit`.

3. **Signature Interactive Blue Dot:**
   * Replaced the centered placeholder with genuine coordinate-based indicators.
   * Each detected object receives an animated pulsing glowing blue dot positioned at its bounding box center.
   * Tapping a dot selects the object, highlights its bounding box, and displays a dedicated details card.

4. **Preserved Day 1 Image Labeling:**
   * General visual concepts and confidence percentages remain accessible via the "Image Labels" tab.

---

## How to Build and Run in Android Studio

1. Launch Android Studio.
2. Select **Open** and choose the folder `D:\PVCA\frontend\android`.
3. Wait for Gradle Sync to complete (Android Studio will resolve dependencies from `libs.versions.toml`).
4. Connect your Android phone with USB debugging enabled.
5. Click the green **Run ▶** button.
