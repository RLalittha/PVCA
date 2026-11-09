# PVCA Android Application

**Project:** PVCA (PayPal Visual Commerce Assistant)  
**Package:** `com.pvca.app`  
**Platform:** Android (Kotlin, Jetpack Compose, Material 3)  
**Current Milestone:** Milestone 3 & 4 (Demo Product Catalogue, Matching Engine & Product Discovery UI)

---

## Architecture Overview (Milestones 3 & 4)

```text
com.pvca.app
├── MainActivity.kt                       # Application entry point (Edge-to-Edge ComponentActivity)
├── data/
│   ├── model/
│   │   ├── ProductItem.kt               # Structured catalogue product entity (prices, merchant, URLs)
│   │   ├── MatchResult.kt               # Matching candidates, confidence tiers, and evidence
│   │   ├── DetectedObjectItem.kt        # Localized object model with bounding box & category
│   │   ├── LabelItem.kt                 # Image-level label domain model with confidence %
│   │   ├── AnalysisOutput.kt            # Aggregated output of dual visual analysis
│   │   └── UiState.kt                   # Sealed interface: Idle, Loading, Success, Empty, Error
│   ├── repository/
│   │   ├── ProductCatalogueRepository.kt# Repository interface for catalogue access
│   │   └── LocalAssetCatalogueRepository.kt # Loads bundled demo_catalogue.json (30 curated products)
│   ├── crop/
│   │   └── ObjectCropAnalyzer.kt        # Crops bounding-box sub-bitmaps for object-specific labeling
│   ├── matching/
│   │   ├── ProductMatchingService.kt    # Weighted ranking engine comparing crops, categories, queries
│   │   └── ExternalSearchAdapter.kt     # Clean abstraction contract for future external retrieval
│   └── analyzer/
│       ├── ImageAnalyzer.kt             # Interface contract for image-level labeling
│       ├── MlKitImageAnalyzer.kt        # ML Kit Image Labeling (On-Device, CPU-friendly)
│       ├── MlKitObjectDetector.kt       # ML Kit Object Detection (Bounding boxes & categories)
│       └── CombinedVisualAnalyzer.kt    # Concurrent coroutine coordinator for dual analysis
└── ui/
    ├── PvcaApp.kt                       # Compose screen coordinating picker, overlay & results
    ├── components/
    │   ├── PvcaHeader.kt                # Brand header ("PVCA: See it. Identify it. Shop it.")
    │   ├── ImagePreviewSection.kt       # Image container with detection overlay & match card
    │   ├── DetectionBoundingBoxOverlay.kt # Bounding box renderer & non-overlapping numbered dots
    │   ├── ProductMatchCard.kt          # Product discovery card with evidence & user refinement
    │   └── AnalysisResultsSection.kt    # Dual-tab layout (Detected Objects & Image Labels)
    ├── util/
    │   └── CoordinateTransformer.kt     # Aspect-ratio aware coordinate mapping (ContentScale.Fit)
    ├── viewmodel/
    │   └── MainViewModel.kt             # StateFlow management with object selection & matching
    └── theme/
        ├── Color.kt                     # PayPal blue (#0070BA) & cyan accent palette
        ├── Type.kt                      # Clean typography definitions
        └── Theme.kt                     # Material 3 light/dark support
```

---

## Key Milestone 3 & 4 Capabilities

1. **Structured Demonstration Product Catalogue (`demo_catalogue.json`):**
   * Curated collection of 30 commercial products spanning Shoes, Handbags, Watches, Eyewear, Apparel, Drinkware, Electronics, and Home Goods.
   * Explicitly labeled as demo data (`"verificationStatus": "demo_verified"`, `"provenance": "Internal PVCA Verified Demo Catalogue"`).
   * Includes specific entries for regression testing (such as the AeroBreeze Ceiling Fan Remote Control).

2. **Object-Specific Crop Analysis (`ObjectCropAnalyzer.kt`):**
   * Solves the critical limitation of full-scene labeling: extracts the exact sub-bitmap defined by the object's bounding box and runs ML Kit Image Labeling specifically on the crop.
   * Prevents assigning background scene labels to the wrong object.

3. **Weighted Product Matching Engine (`ProductMatchingService.kt`):**
   * Transparent ranking combining crop visual labels, ML Kit category alignment, scene context, and optional user description terms.
   * Three distinct confidence tiers: `HIGH_CONFIDENCE`, `POSSIBLE_MATCH`, and `NO_RELIABLE_MATCH`.
   * Formulates human-readable evidence explanations (e.g., *"Visual crop matched keywords: Remote control, Electronics"*).

4. **User-Assisted Search Fallback (Phase C):**
   * Integrated refinement input inside [`ProductMatchCard.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/ui/components/ProductMatchCard.kt).
   * If a user wants to specify an exact variant (e.g., *"brown leather handbag"*), the engine re-ranks candidates and badges the outcome as *"User Refined"*.

5. **Elimination of Overlapping Label Clutter (Phase E):**
   * Removed bulky floating text badges above every bounding box.
   * Each interactive blue dot now displays a crisp, centered numeric badge (`1`, `2`, `3`) with a pulsing halo.
   * Detailed product match information is surfaced cleanly in the dedicated card below the preview.

---

## How to Build and Run in Android Studio

1. Open Android Studio.
2. Open `D:\PVCA\frontend\android`.
3. Wait for Gradle Sync to complete.
4. Connect your Motorola Android phone (or test device) with USB debugging enabled.
5. Click the green **Run ▶** button.
