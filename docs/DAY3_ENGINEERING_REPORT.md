# PVCA — Day 3 Engineering Report
## Milestones 3, 4 & 5: Visual Identity Refinement, Demonstration Catalogue & On-Device Matching Engine

**Project:** PVCA (PayPal Visual Commerce Assistant)  
**Package:** `com.pvca.app`  
**Platform:** Android (Kotlin, Jetpack Compose, Material 3)  
**Date:** November 9, 2026  
**Target Milestones:** 
- **Milestone 3:** Visual Identity Refinement & Blue Dot Polishing
- **Milestone 4:** Local Verified Demonstration Product Catalogue
- **Milestone 5:** On-Device Product Matching Engine & User Refinement Fallback  
**Document Status:** Complete & Verified on Physical Hardware  
**Target Device:** Motorola Android Smartphone (Physical Hardware)  
**Suggested Commit:** `feat: Complete Day 3 product matching and mobile UI`

---

## 1. Executive Summary & Key Accomplishments

Day 3 transitions PVCA from an isolated spatial detector into an integrated visual commerce assistant. By combining object-specific sub-crop analysis, a 30-product demonstration catalogue, transparent ranking mathematics, and user-assisted search refinement, PVCA connects visual objects on-screen to concrete product records with explicit provenance and honest uncertainty indicators.

### Key Achievements:

1. **Multi-Object Detection & Numbered Blue Dots (Milestone 3):**
   * Eliminated floating badge overlap from multi-object scenes by embedding crisp numeric indicators (`#1`, `#2`, `#3`) directly inside the 24dp core of each interactive blue dot at the object's geometric center.
   * Rendered unselected detections with a clean dashed white outline (alpha 0.50), while illuminating active selected items with a solid 3dp cyan border and translucent tint.

2. **Demonstration Product Catalogue (Milestone 4):**
   * Curated 30 commercial products in `demo_catalogue.json` across Footwear, Bags, Watches, Consumer Electronics, Eyewear, Drinkware, and Apparel.
   * Enforced clean data integrity: explicit demonstration pricing, seller metadata, verification tags (`"demo_verified"`), and safety checks against unverified URLs.
   * Built `LocalAssetCatalogueRepository` with JSON streaming, caching, and token-based search.

3. **Object-Specific Crop Analysis (`ObjectCropAnalyzer`):**
   * Solved the critical limitation of full-scene labeling: extracts the bounding-box sub-bitmap (with EXIF rotation awareness) and runs ML Kit Image Labeling directly on the isolated product.
   * Prevents assigning background scene labels (e.g. tables, rooms, flooring) to foreground products.

4. **Weighted Hybrid Matching Engine (`ProductMatchingService`):**
   * Transparent scoring algorithm combining crop labels (60%), ML Kit category alignment (25%), and scene context (15%).
   * Strictly enforces honest, non-misleading confidence tiers:
     * **Demo Match (High Confidence)** (`score >= 0.58`)
     * **Possible Match** (`0.32 <= score < 0.58`)
     * **No Reliable Match** (`score < 0.32`)
   * Generates natural language evidence explanations detailing why an item matched.

5. **User-Assisted Search Fallback (Phase C):**
   * Added an inline refinement input inside `ProductMatchCard`.
   * Users can enter clarifying keywords (e.g., *"brown leather handbag"* or *"fan remote control"*), re-ranking candidates with query boosting (50% user tokens) and badging the outcome as `"User Refined"`.

6. **Mobile UI Bug Fixes & Layout Stabilization:**
   * **Fixed Vertically Stretched Store Button:** Replaced a cramped horizontal `Row` with a responsive vertical `Column(modifier = Modifier.fillMaxWidth())`. The demo notice sits on its own line and the store button spans full width with single-line horizontal text, eliminating character-by-character vertical wrapping on narrow screens.
   * **Clean Remote Control Placeholders:** Cleared external photo references for the AeroBreeze fan remote (`PVCA-ELEC-001`) and TV remote (`PVCA-ELEC-002`), allowing `ProductCategoryPlaceholder` to render the dedicated `Icons.Default.SettingsRemote` vector icon inside the themed container instead of mismatched consumer electronics photos.
   * **Safe Store Link Disabling:** External checkout links are disabled (`enabled = false`) for demonstration products with an explanatory notice (`"Store Link Disabled (Demo)"`), preventing broken browser navigation (`ERR_NAME_NOT_RESOLVED`).

7. **Compilation & Comprehensive Unit Testing:**
   * Fixed Coil `AsyncImage` argument type errors by migrating to `SubcomposeAsyncImage` with composable `error` and `loading` slots.
   * Configured desktop JVM test mocking with `isReturnDefaultValues = true` and `org.json:json:20240303`.
   * Build succeeded: `:app:assembleDebug` completed in 26s.
   * Test suite succeeded: all **20 unit tests passed** in 11s.

---

## 2. Technical Architecture & Component Structure

```text
com.pvca.app
├── MainActivity.kt                       # Edge-to-Edge ComponentActivity entry point
├── data/
│   ├── model/
│   │   ├── ProductItem.kt               # Catalogue product entity (prices, merchant, URLs, provenance)
│   │   ├── MatchResult.kt               # Sealed match outcomes, candidates & confidence tiers
│   │   ├── DetectedObjectItem.kt        # Localized object domain model with bounding boxes & category
│   │   ├── LabelItem.kt                 # Image-level label domain model with confidence % (Day 1)
│   │   ├── AnalysisOutput.kt            # Aggregated visual analysis output
│   │   └── UiState.kt                   # Sealed UI state: Idle, Loading, Success, Empty, Error
│   ├── repository/
│   │   ├── ProductCatalogueRepository.kt# Repository contract for product data access
│   │   └── LocalAssetCatalogueRepository.kt # Asset-backed JSON catalogue repository
│   ├── crop/
│   │   └── ObjectCropAnalyzer.kt        # EXIF-aware bounding box cropper and sub-image labeler
│   ├── matching/
│   │   ├── ProductMatchingService.kt    # Weighted ranking engine and confidence classifier
│   │   └── ExternalSearchAdapter.kt     # Abstraction contract for future external retrieval
│   └── analyzer/
│       ├── ImageAnalyzer.kt             # Interface contract for image labeling
│       ├── MlKitImageAnalyzer.kt        # On-device ML Kit image labeling
│       ├── MlKitObjectDetector.kt       # On-device ML Kit object detector
│       └── CombinedVisualAnalyzer.kt    # Concurrent coroutine coordinator
└── ui/
    ├── PvcaApp.kt                       # Master Compose coordinator
    ├── components/
    │   ├── PvcaHeader.kt                # Brand header ("PVCA: See it. Identify it. Shop it.")
    │   ├── ImagePreviewSection.kt       # Preview container hosting detection overlay & match card
    │   ├── DetectionBoundingBoxOverlay.kt # Clean bounding boxes & numbered pulsing blue dots
    │   ├── ProductMatchCard.kt          # Product match card with evidence, status & user refinement
    │   ├── SelectedObjectCard.kt        # Fallback raw object metadata card
    │   └── AnalysisResultsSection.kt    # Dual-tab layout (Detected Objects & Image Labels)
    ├── util/
    │   └── CoordinateTransformer.kt     # Screen coordinate transformation engine
    ├── viewmodel/
    │   └── MainViewModel.kt             # StateFlow orchestration for visual analysis and matching
    └── theme/
        ├── Color.kt                     # PayPal Blue (#0070BA), Cyan (#00CFDE), and semantic colors
        ├── Type.kt                      # Typography definitions
        └── Theme.kt                     # Material 3 light/dark theme
```

---

## 3. Files Created or Modified

| File | Status | Description |
|---|---|---|
| [`demo_catalogue.json`](file:///D:/PVCA/frontend/android/app/src/main/assets/demo_catalogue.json) | **Created** | 30 curated demonstration products with clean placeholders for remotes. |
| [`ProductItem.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/data/model/ProductItem.kt) | **Created** | Domain model for catalogue products with formatting and live URL verification. |
| [`MatchResult.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/data/model/MatchResult.kt) | **Created** | Domain models for matching candidates, confidence tiers, and sealed outcomes. |
| [`ProductCatalogueRepository.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/data/repository/ProductCatalogueRepository.kt) | **Created** | Interface abstraction for product catalogue access. |
| [`LocalAssetCatalogueRepository.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/data/repository/LocalAssetCatalogueRepository.kt) | **Created** | Asset-backed repository with JSON streaming, caching, and token search. |
| [`ObjectCropAnalyzer.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/data/crop/ObjectCropAnalyzer.kt) | **Created** | EXIF-aware bounding box cropper and object-specific ML Kit labeler. |
| [`ProductMatchingService.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/data/matching/ProductMatchingService.kt) | **Created** | Weighted matching engine, category alignment, and evidence builder. |
| [`ExternalSearchAdapter.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/data/matching/ExternalSearchAdapter.kt) | **Created** | Architectural contract for future external retrieval. |
| [`ProductMatchCard.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/ui/components/ProductMatchCard.kt) | **Created** | Compose discovery card with `SubcomposeAsyncImage`, responsive full-width disabled button, and user refinement. |
| [`DetectionBoundingBoxOverlay.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/ui/components/DetectionBoundingBoxOverlay.kt) | Modified | Removed cluttering pill badges; added numeric IDs inside blue dots; subtle dashed boxes. |
| [`ImagePreviewSection.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/ui/components/ImagePreviewSection.kt) | Modified | Integrated `ProductMatchCard` and wired user query callbacks. |
| [`MainViewModel.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/ui/viewmodel/MainViewModel.kt) | Modified | Coordinated crop analysis, catalogue matching, and user refinement states. |
| [`PvcaApp.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/ui/PvcaApp.kt) | Modified | Passed context and wired user refinement callbacks between preview and viewmodel. |
| [`CoordinateTransformer.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/ui/util/CoordinateTransformer.kt) | Modified | Direct field assignments on `RectF` for local unit test compatibility. |
| [`DetectedObjectItem.kt`](file:///D:/PVCA/frontend/android/app/src/main/java/com/pvca/app/data/model/DetectedObjectItem.kt) | Modified | Implemented `widthPx` and `heightPx` using field subtractions. |
| [`app/build.gradle.kts`](file:///D:/PVCA/frontend/android/app/build.gradle.kts) | Modified | Added `isReturnDefaultValues = true` and `org.json:json:20240303` for desktop unit testing. |
| [`ProductMatchingEngineTest.kt`](file:///D:/PVCA/frontend/android/app/src/test/java/com/pvca/app/matching/ProductMatchingEngineTest.kt) | **Created** | Unit tests for high-confidence matching, user refinement, and no-match handling. |
| [`LocalAssetCatalogueRepositoryTest.kt`](file:///D:/PVCA/frontend/android/app/src/test/java/com/pvca/app/repository/LocalAssetCatalogueRepositoryTest.kt) | **Created** | Unit tests for catalogue JSON parsing, malformed entry filtering, and token search. |
| [`MainViewModelTest.kt`](file:///D:/PVCA/frontend/android/app/src/test/java/com/pvca/app/viewmodel/MainViewModelTest.kt) | Modified | Unit tests covering match result state flows. |
| [`CoordinateTransformerTest.kt`](file:///D:/PVCA/frontend/android/app/src/test/java/com/pvca/app/util/CoordinateTransformerTest.kt) | Modified | Unit tests updated with field-initialized rects. |
| [`DetectedObjectItemTest.kt`](file:///D:/PVCA/frontend/android/app/src/test/java/com/pvca/app/model/DetectedObjectItemTest.kt) | Modified | Unit tests updated with field-initialized rects. |
| [`frontend/android/README.md`](file:///D:/PVCA/frontend/android/README.md) & [`README.md`](file:///D:/PVCA/README.md) | Modified | Updated documentation reflecting Milestones 3, 4, and 5. |

---

## 4. Verification & Testing

### 4.1 Unit Test Suite (20 Tests Passed)
* **`ProductMatchingEngineTest` (3 tests):**
  * `cropLabelsMatchingRemote_returnsRemoteWithHighConfidence`: PASSED.
  * `userAssistedQuery_boostsAndRefinesCandidate`: PASSED.
  * `unrelatedCategoryAndNoCrop_returnsNoMatch`: PASSED.
* **`LocalAssetCatalogueRepositoryTest` (3 tests):**
  * `parseProductsJson_filtersOutMalformedEntries`: PASSED.
  * `getProductById_returnsCorrectProduct`: PASSED.
  * `searchProducts_matchesKeywordsAndCategory`: PASSED.
* **`CoordinateTransformerTest` (5 tests):**
  * Aspect-ratio scaling, pillarboxing, letterboxing, normalization, and zero-dimension safety: ALL PASSED.
* **`DetectedObjectItemTest` (3 tests):**
  * Confidence rounding, null handling, tracking ID preservation: ALL PASSED.
* **`LabelItemTest` (3 tests):**
  * Percentage formatting, confidence boundaries: ALL PASSED.
* **`MainViewModelTest` (3 tests):**
  * Idle state, clear selection, object selection updates: ALL PASSED.

### 4.2 Physical Hardware Validation (Motorola Android Smartphone)
* **Fan Remote Test Case:**
  * Selecting the fan remote photo successfully detects the object, positions blue dot `#1` at its geometric center, and highlights its bounding box.
  * The match card displays:
    * Title: `AeroBreeze 3-Speed Ceiling Fan Remote Control`
    * Category: `Consumer Electronics`
    * Status Badge: `Demo Match (High Confidence)` (Green)
    * Thumbnail: Clean `Icons.Default.SettingsRemote` category icon on themed background.
    * Store Button: Single-line horizontal `Store Link Disabled (Demo)` button spanning full width without wrapping.
    * Evidence: *"Visual crop matched keywords: Remote control, Electronics"*.
* **Multi-Object Scenes:**
  * Multi-object photos render cleanly without badge clutter. Blue dots are numbered `#1`, `#2`, `#3`, `#4` with interactive selection.
* **Manual Search Refinement:**
  * Entering descriptive terms in the text field re-ranks catalogue entries dynamically and displays the `"User Refined"` badge.

---

## 5. Known Limitations & Technical Observations

1. **Category Semantic Overlap (Drinkware Ambiguity):**
   * *Observation:* During physical device testing, a photograph of a water bottle was suggested as an `Artisan Speckled Ceramic Coffee Mug` instead of a bottle.
   * *Root Cause:* Both items share broad visual labels ("Tableware", "Drinkware", "Cup", "Container") and coarse ML Kit categories (`Home good`). Because standard on-device image labeling does not perform geometric aspect-ratio shape fitting or OCR brand reading, the weighted ranking scored the coffee mug slightly higher based on broad keyword overlap.
   * *Mitigation in Milestone 5/6:* In upcoming milestones, this limitation will be resolved by incorporating OCR-based text/brand extraction, fine-grained shape embeddings, and server-side visual similarity search.
2. **Coarse ML Kit Object Taxonomy:** The on-device detector categorizes items into 5 broad classes (`Fashion good`, `Home good`, `Food`, `Place`, `Plant`). Exact product differentiation relies heavily on crop analysis and user refinement.
3. **Static Image Mode:** Temporal video tracking across moving frames remains scheduled for future screen-capture milestones.
4. **No Live Payments or Merchant Checkouts:** All prices and links are demonstration-only; PayPal checkout integration is intentionally scheduled for Milestone 7.

---

## 6. Next Steps (Milestone 6: FastAPI Backend)

With on-device object detection, coordinate-accurate blue dots, crop analysis, local catalogue matching, and mobile UI layout finalized and verified on physical hardware:
* **Milestone 6 (FastAPI Backend):**
  * Establish the Python FastAPI server under `backend/`.
  * Implement server-side product catalogue endpoints (`GET /api/v1/products`).
  * Implement server-side matching API (`POST /api/v1/match`).
  * Define documented OpenAPI contracts under `docs/api.md`.
  * Ensure server-authoritative pricing and merchant verification in preparation for PayPal Sandbox integration (Milestone 7).
