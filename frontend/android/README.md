# PVCA Android Application

**Project:** PVCA (PayPal Visual Commerce Assistant)  
**Package:** `com.pvca.app`  
**Platform:** Android (Kotlin, Jetpack Compose, Material 3)  
**Current Milestone:** Milestone 0 & Milestone 1 (Foundation + Photo Picker + ML Kit Image Labeling)

---

## Architecture Overview

```text
com.pvca.app
├── MainActivity.kt                # Application entry point (Edge-to-Edge ComponentActivity)
├── data/
│   ├── model/
│   │   ├── LabelItem.kt          # Domain model with confidence percentage conversion
│   │   └── UiState.kt            # Sealed interface: Idle, Loading, Success, Empty, Error
│   └── analyzer/
│       ├── ImageAnalyzer.kt      # Abstraction contract for visual intelligence
│       └── MlKitImageAnalyzer.kt # Google ML Kit Image Labeling (On-Device, CPU-friendly)
└── ui/
    ├── PvcaApp.kt                # Compose screen coordinating picker & sections
    ├── components/
    │   ├── PvcaHeader.kt         # Brand header ("PVCA: See it. Identify it. Shop it.")
    │   ├── ImagePreviewSection.kt# Photo Picker trigger, image rendering & temp blue-dot placeholder
    │   └── AnalysisResultsSection.kt # Modular states for label items & confidence progress bars
    ├── viewmodel/
    │   └── MainViewModel.kt      # Coroutine-powered StateFlow management
    └── theme/
        ├── Color.kt              # PayPal blue (#0070BA) & cyan accent palette
        ├── Type.kt               # Clean typography definitions
        └── Theme.kt              # Material 3 light/dark support
```

---

## Prerequisites for Building & Running

1. **Android Studio**: Install [Android Studio Ladybug or Koala](https://developer.android.com/studio).
   - Android Studio automatically provides the required **Java 17 / 21 (JetBrains Runtime)** and Android SDK.
2. **Android SDK**:
   - Compile SDK: `34` (Android 14) or `35` (Android 15)
   - Minimum SDK: `24` (Android 7.0 Nougat)
   - Target SDK: `34`
3. **Physical Device or Emulator**:
   - Enable **USB Debugging** on your physical Android phone (Developer Options).
   - Alternatively, create an Android Virtual Device (AVD) in Android Studio Device Manager.

---

## How to Open and Run in Android Studio

1. Launch Android Studio.
2. Select **Open** and choose the folder `D:\PVCA\frontend\android`.
3. Wait for Gradle Sync to complete (Android Studio will automatically download Gradle 8.7, Android Gradle Plugin 8.5.2, and dependencies).
4. Connect your Android phone with a USB cable (or start an emulator).
5. Click the green **Run ▶** button in Android Studio.

---

## Important Technical Limitations (Milestone 1)

> **Clarification:** Google ML Kit Image Labeling provides **image-level labels** (e.g., "Shoe", "Watch", "Footwear"). It does **not** provide object bounding boxes.
> 
> The blue dot currently visible on the selected image is a **temporary UI placeholder** demonstrating the visual style and future location of the indicator. Real object localization with bounding boxes will be implemented in **Milestone 2**.
