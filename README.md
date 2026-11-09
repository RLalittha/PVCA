# PVCA — PayPal Visual Commerce Assistant

> **See it. Identify it. Shop it.**
> 
> *An AI-powered visual commerce assistant connecting on-screen product discovery with seamless PayPal checkout.*

---

## Project Status

- **Deadline:** November 12, 2026
- **Current Phase:** Android Proof of Concept (Day 3)
- **Active Milestones:**
  - [x] **Milestone 0 — Application Foundation:** Android project structure created with Kotlin, Jetpack Compose, Material 3, and Gradle Version Catalog.
  - [x] **Milestone 1 — Image Selection & AI Image Labeling:** Android Photo Picker integrated, Google ML Kit Image Labeling integrated.
  - [x] **Milestone 2 — Object Detection, Bounding Boxes & Interactive Blue Dot:** On-device ML Kit object localization, letterbox-aware coordinate transformation, and interactive glowing blue dot selection.
  - [x] **Milestone 3 — Visual Identity & Blue Dot Polishing:** Refined non-overlapping numbered blue dots and uncluttered bounding box overlays.
  - [x] **Milestone 4 — Local Verified Product Catalogue & Matching Engine:** 30 curated demonstration products, object-specific crop analysis, weighted ranking, and user-assisted search fallback.
  - [ ] **Milestone 5 — Advanced Hybrid Matching & External Search Integration:** (Upcoming)
  - [ ] **Milestone 6 — FastAPI Backend:** (Upcoming)
  - [ ] **Milestone 7 — PayPal Sandbox Integration:** (Upcoming)

---

## Repository Structure

```text
PVCA/
├── AGENTS.md                  # Operational rules and role definitions
├── MASTER_HANDOFF.md          # Canonical project vision and architectural decisions
├── frontend/
│   └── android/               # Android client (Kotlin + Jetpack Compose + ML Kit)
│       ├── app/               # Application module (com.pvca.app)
│       └── README.md          # Android build and run guide
└── docs/                      # Architectural specifications
```

---

## Canonical Documents

- [MASTER_HANDOFF.md](file:///D:/PVCA/MASTER_HANDOFF.md): Master specifications and architectural requirements.
- [AGENTS.md](file:///D:/PVCA/AGENTS.md): Coding rules, security boundaries, and instructions.
