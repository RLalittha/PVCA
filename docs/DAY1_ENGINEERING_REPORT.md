# PVCA — Day 1 Engineering Report & Project Inspection

**Project:** PVCA (PayPal Visual Commerce Assistant)  
**Package:** `com.pvca.app`  
**Platform:** Android (Kotlin, Jetpack Compose, Material 3)  
**Current Milestone Target:** Milestone 0 (Foundation) & Milestone 1 (Image Selection + On-Device AI Image Labeling)  
**Document Status:** Complete & Verified on Physical Hardware  
**Git Checkpoint:** `d64ccde` ("Day 1: Working PVCA Android app with ML Kit image labeling")

---

## 1. Project & Environment Inspection (Phase 1)

Before writing any implementation code, a complete inspection of the repository and development machine was conducted in accordance with `MASTER_HANDOFF.md` and `AGENTS.md`.

### 1.1 Repository Inspection
* **Location:** `D:\PVCA`
* **Version Control:** Git repository initialized on branch `main` with no prior commits.
* **Pre-existing Files:**
  * `MASTER_HANDOFF.md`: The canonical source of truth for product vision, architecture, and milestones.
  * `AGENTS.md`: Operational guidelines, frontend/backend separation rules, and technical constraints.
* **Existing Android Implementation:** No Android project, Gradle build scripts, manifest, or Kotlin source code existed in the repository prior to Day 1.

### 1.2 Host Environment Findings
* **Operating System:** Windows 11 (64-bit).
* **Hardware:** Intel Core i3-1215U (12th Generation, CPU-only, 8 GB RAM, ~477 GB storage).
* **Java:** Legacy Oracle Java 8 (`jdk1.8.0_191`) was present in `C:\Program Files\Java\jdk1.8.0_191`.
  * *Requirement note:* Modern Android Gradle Plugin (AGP 8.0+) requires **JDK 17 or higher**.
* **Android Tools:** Android Studio, Android SDK, and `adb` were not initially found in the system PATH.
* **Other Tools:** Git (`C:\Program Files\Git\cmd\git.exe`) and Python 3.14 were installed and operational.

---

## 2. Architecture & Implementation (Milestones 0 & 1)

In alignment with Milestone 0 and Milestone 1, the Android client codebase was established under `frontend/android/` using clean architecture, idiomatic Kotlin, Jetpack Compose, and Material 3.

### 2.1 Codebase Structure
```text
com.pvca.app
├── MainActivity.kt                       # Entry point using Edge-to-Edge ComponentActivity
├── data/
│   ├── model/
│   │   ├── LabelItem.kt                 # Domain data class with percentage conversion
│   │   └── UiState.kt                   # Sealed UI states: Idle, Loading, Success, Empty, Error
│   └── analyzer/
│       ├── ImageAnalyzer.kt             # Interface contract for visual analysis
│       └── MlKitImageAnalyzer.kt        # Google ML Kit Image Labeling (On-Device, CPU-friendly)
└── ui/
    ├── PvcaApp.kt                       # Main composable screen coordinating photo picker & UI
    ├── components/
    │   ├── PvcaHeader.kt                # Brand header ("PVCA: See it. Identify it. Shop it.")
    │   ├── ImagePreviewSection.kt       # Image preview, Photo Picker trigger & placeholder blue dot
    │   └── AnalysisResultsSection.kt    # Dedicated results area with confidence progress bars
    ├── viewmodel/
    │   └── MainViewModel.kt             # Asynchronous StateFlow state holder
    └── theme/
        ├── Color.kt                     # PayPal Blue (#0070BA), Deep Navy, and Cyan Accent
        ├── Type.kt                      # Modern typography
        └── Theme.kt                     # Material 3 light & dark theme configurations
```

### 2.2 Android Photo Picker Integration
* Utilizes `rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())`.
* **Zero Intrusive Permissions:** The modern Android Photo Picker runs in a system process and does not require runtime storage permissions (`READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES`), respecting user privacy.
* **Edge Case Safety:** Cancelling the photo picker returns a `null` Uri; the ViewModel handles this gracefully without resetting loaded state or crashing.

### 2.3 Google ML Kit Image Labeling
* Implemented in `MlKitImageAnalyzer.kt` using `com.google.mlkit:image-labeling:17.0.9`.
* Executes locally on the CPU (compatible with the Core i3-1215U machine) using `ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)`.
* Fully asynchronous via Kotlin Coroutines on `Dispatchers.IO` using `kotlinx-coroutines-play-services` (`await()`), preventing UI stutters.
* Maps detected `ImageLabel` entities to domain `LabelItem` records sorted in descending order of confidence.
* Encapsulates complete state flow:
  * `UiState.Idle`: Guidance prompt before image selection.
  * `UiState.Loading`: Visual progress indicator and status label during inference.
  * `UiState.Success`: List of detected labels, confidence percentages, and visual meters.
  * `UiState.Empty`: Informative notice if no labels meet confidence criteria.
  * `UiState.Error`: Graceful error banner if an image cannot be read or analyzed.

### 2.4 Technical Honesty: The Blue-Dot UI Placeholder
As dictated by `MASTER_HANDOFF.md`:
> **Google ML Kit Image Labeling provides image-level labels; it does not provide bounding boxes.**
>
> An interactive blue dot positioned over a specific object requires true coordinate-based localization. Therefore, the blue dot displayed in Milestone 1 is explicitly badged and styled as a **temporary UI placeholder** positioned at the center of the preview. The UI includes an explanatory disclaimer card stating that real object localization arrives in **Milestone 2**.

---

## 3. Files Created

| File | Purpose |
|---|---|
| `.gitignore` | Prevents tracking of Gradle caches, Android Studio files, APKs, and secrets. |
| `README.md` | Root overview of the PVCA vision, milestones, and directory structure. |
| `frontend/android/README.md` | Detailed setup and execution guide for the Android project. |
| `frontend/android/gradle/libs.versions.toml` | Centralized Gradle Version Catalog managing versions and dependencies. |
| `frontend/android/build.gradle.kts` | Root build configuration applying plugins lazily. |
| `frontend/android/settings.gradle.kts` | Plugin and repository management, includes `:app`. |
| `frontend/android/gradle.properties` | JVM memory settings (`-Xmx2048m`) and AndroidX properties. |
| `frontend/android/gradle/wrapper/gradle-wrapper.properties` | Points to Gradle 8.7 distribution. |
| `frontend/android/app/build.gradle.kts` | Application module configuration (compileSdk 34, minSdk 24, Compose, ML Kit). |
| `frontend/android/app/proguard-rules.pro` | R8/ProGuard configuration rules. |
| `frontend/android/app/src/main/AndroidManifest.xml` | App manifest defining launcher activity. |
| `frontend/android/app/src/main/res/values/*` | Strings, color palettes, and themes. |
| `frontend/android/app/src/main/res/drawable/*` & `mipmap-*` | Vector adaptive launcher icons featuring PVCA branding. |
| `MainActivity.kt` | Application entry point setting Edge-to-Edge and Compose content. |
| `LabelItem.kt` | Domain model with integer percentage formatting. |
| `UiState.kt` | Sealed interface representing Idle, Loading, Success, Empty, and Error states. |
| `ImageAnalyzer.kt` | Clean interface abstraction for visual analysis engines. |
| `MlKitImageAnalyzer.kt` | ML Kit Image Labeling implementation using coroutines. |
| `MainViewModel.kt` | ViewModel managing StateFlows for image Uri and UI state. |
| `PvcaApp.kt` | Root Composable layout assembling header, picker, and results. |
| `PvcaHeader.kt` | Branded header displaying "PVCA: See it. Identify it. Shop it." |
| `ImagePreviewSection.kt` | Image preview card, Photo Picker launcher, and temporary blue-dot placeholder. |
| `AnalysisResultsSection.kt` | Dedicated visual analysis card displaying label rows and progress bars. |
| Theme (`Color.kt`, `Type.kt`, `Theme.kt`) | Material 3 dark/light themes with PayPal Blue accents. |
| Unit Tests (`LabelItemTest.kt`, `MainViewModelTest.kt`) | Unit tests verifying domain logic, percentage rounding, and state transitions. |
| `frontend/android/verify_setup.ps1` | Environment diagnostic script checking Java, Android Studio, SDK, and ADB. |

---

## 4. Dependencies Added

All dependencies use stable, mutually compatible versions defined in `frontend/android/gradle/libs.versions.toml`:

* `com.android.application:8.5.2`: Android Gradle Plugin.
* `org.jetbrains.kotlin.android:2.0.0`: Kotlin compiler plugin with integrated Compose compiler.
* `androidx.compose.material3:material3` via Compose BOM `2024.09.00`: Material 3 design system.
* `androidx.activity:activity-compose:1.9.2`: Provides standard `PickVisualMedia` contract.
* `com.google.mlkit:image-labeling:17.0.9`: On-device AI image labeling running locally on CPU.
* `org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1`: Provides `.await()` extension on Play Services `Task` objects.
* `io.coil-kt:coil-compose:2.7.0`: Asynchronous image loading for Compose.
* `junit:junit:4.13.2`: Unit testing framework.

---

## 5. Verification Update & Hardware Validation

Following project setup in Android Studio:

* **Gradle Synchronization:** Succeeded with all dependencies resolved via Gradle 8.7 and AGP 8.5.2.
* **Compilation & Build:** The Android application built successfully with zero compilation errors.
* **Physical Device Deployment:** The app was deployed and launched on a physical Android phone with USB debugging enabled.
* **Photo Picker Functionality:** Verified working — pressing "Select Product Image" opens the native Android Photo Picker, and selecting an image passes the content Uri to the app.
* **Image Display:** Verified working — Coil renders the selected image clearly in the preview area.
* **On-Device AI Analysis:** Verified working — Google ML Kit processes the image on-device and returns visual concept labels along with confidence percentages.
* **Git Version Control:** All Day 1 files were staged and committed to Git (Commit `d64ccde`: *"Day 1: Working PVCA Android app with ML Kit image labeling"*).

### Tested vs. Untested Boundaries
* **Verified Tested:**
  * App launch and rendering on a physical Android phone.
  * Opening Android Photo Picker and selecting an image.
  * Image preview rendering.
  * ML Kit label extraction on normal product images.
  * Display of labels and confidence percentages in the UI.
* **Remaining Untested / Future Scope:**
  * Complex edge cases such as corrupted image files, unsupported MIME types, or extreme low-light/occluded image inputs.
  * High-frequency sequential image switching stress testing.
  * Multi-window and orientation change lifecycle retention tests.

---

## 6. Known Limitations (Milestone 1 Scope)

1. **No Object Bounding Boxes:** Current analysis relies on ML Kit Image Labeling (image-level classification). It does not provide `(x, y, width, height)` bounding boxes.
2. **Centered Placeholder Dot:** The blue indicator shown over the image is a fixed, pulsing visual placeholder indicating the future location of the interactive element.
3. **No Catalogue or Backend:** No product matching, local demonstration catalogue, FastAPI backend, or PayPal payment integrations are active in this milestone.

---

## 7. Day 2 Readiness & Next Recommended Milestone

With Milestone 0 and Milestone 1 fully verified on a physical device, the project is ready for **Milestone 2**:

* **Milestone 2 Objective:** Actual Object Detection with Bounding Boxes.
* **Next Steps:**
  1. Integrate an on-device object detection model (e.g., ML Kit Object Detection & Tracking).
  2. Retrieve real normalized bounding boxes `(left, top, right, bottom)`.
  3. Map the detected bounding-box coordinates to the Compose image preview viewport.
  4. Position the blue dot directly over the detected product rather than at the center of the image.
