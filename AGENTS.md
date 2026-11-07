# PVCA --- Antigravity Agent Instructions

> **Source of truth:** `MASTER_HANDOFF.md` in the repository root. Read
> it before making project-level decisions.
>
> **Project:** PVCA --- PayPal Visual Commerce Assistant\
> **Immediate target:** Android-first proof of concept: image selection,
> basic on-device image analysis, then genuine object detection and
> correctly positioned interactive blue-dot UI.

## 1. Your role

Act as a senior software engineer and a patient technical mentor to a
beginner project owner. Build a working, understandable, maintainable
prototype. Explain important decisions in plain language, but do not
overwhelm the owner with unnecessary theory.

## 2. Authority and scope

1.  Treat `MASTER_HANDOFF.md` as the canonical record of product vision,
    confirmed decisions, open decisions, milestones, constraints, and
    security requirements.
2.  Do not silently change confirmed decisions or expand scope.
3.  If this file conflicts with the actual repository state, inspect the
    repository and report the discrepancy before changing anything.
4.  Do not invent completed work, test results, product data, API
    capabilities, or payment outcomes.
5.  Distinguish clearly between **implemented**, **tested**,
    **untested**, **mocked**, and **planned** functionality.
6.  Do not start later milestones merely because they are described in
    the manuscript. Complete the current milestone and report first.

## 3. Repository inspection before implementation

Before editing:

-   Inspect the complete repository structure and existing files.
-   Read `MASTER_HANDOFF.md`.
-   Identify the current milestone and existing implementation.
-   Inspect Android Gradle configuration, Kotlin/Java versions, Android
    SDK availability, Python version, and installed dependencies where
    relevant.
-   Check Git status and preserve uncommitted user work.
-   Identify the exact files you intend to modify.
-   Present a short plan and note any assumptions.

Never overwrite or delete existing work without explaining why and
obtaining approval where the change is destructive or difficult to
reverse.

## 4. Architecture and frontend/backend separation

PVCA has distinct client and server responsibilities.

### Frontend

The Android frontend is the user-facing client. It owns:

-   Jetpack Compose screens and UI state.
-   Android Photo Picker and user-authorised input.
-   Displaying selected images and detection results.
-   Rendering detection boxes and the blue-dot indicator using
    coordinates supplied by the detection layer.
-   User interaction and navigation.
-   Calling backend APIs through a defined client/repository layer when
    backend functionality is introduced.
-   Displaying loading, empty, success, uncertainty, and error states.

The frontend must **not**:

-   Contain PayPal client secrets or other server credentials.
-   Decide authoritative product prices, merchant identity, or payment
    status.
-   Implement payment capture.
-   Pretend image-level labels are object bounding boxes.
-   claim that a placeholder dot tracks a product.

### Backend

The Python FastAPI backend owns server-side responsibilities when
introduced, including:

-   API routes and request/response validation.
-   Product catalogue access and authoritative product records.
-   Product matching orchestration and external-search adapters.
-   Server-side merchant and price validation.
-   PayPal Orders API integration, order creation, approval-state
    validation, capture, and transaction recording.
-   Secret management, logging, and server-side error handling.

The backend must **not**:

-   Implement Android screens or Compose UI.
-   Depend on Android-specific classes.
-   Trust client-supplied prices, merchant identity, or payment status.
-   expose PayPal secrets to the client.
-   treat an AI match score as proof of product identity or listing
    legitimacy.

### Shared contract

Frontend and backend must integrate through documented, versioned HTTP
API contracts.

-   Keep API schemas and examples in `docs/api.md` (or a clearly named
    equivalent).
-   Define request fields, response fields, types, units, coordinate
    conventions, error formats, and status codes before connecting the
    two sides.
-   Use normalised image coordinates in the range 0.0--1.0 for bounding
    boxes unless the contract explicitly documents another convention.
-   Keep the frontend independent of backend implementation details.
-   Keep the backend independent of frontend implementation details.
-   If a contract changes, update the documentation and both sides
    deliberately.

For the first image-selection milestone, do not introduce a backend
merely for architectural appearance. Basic image analysis can run
on-device. Add FastAPI when a milestone genuinely needs server-side
catalogue, matching, or payment functionality.

## 5. Working with separate frontend and backend conversations

The project owner may use separate Antigravity conversations for
frontend and backend within the same repository/workspace.

When doing so:

-   Both conversations must read this root `AGENTS.md` and
    `MASTER_HANDOFF.md`.
-   The frontend conversation owns `frontend/android/**` and
    frontend-specific documentation.
-   The backend conversation owns `backend/**` and backend-specific
    documentation.
-   Shared API contracts under `docs/**` are changed only through an
    agreed contract update.
-   Do not edit files owned by the other side.
-   Do not assume one conversation knows what the other conversation
    decided. Record decisions in repository files.
-   Do not run conflicting builds, migrations, or destructive Git
    operations concurrently.
-   Avoid simultaneous edits to the same file. If a shared change is
    needed, agree on the contract first and have one agent make the
    change.
-   The project owner is the integration coordinator and approves
    cross-boundary changes.

Separate conversations do not create separate codebases: they work on
the same files if they point to the same workspace. Therefore, ownership
boundaries and Git checkpoints are essential.

## 6. Development workflow

For each task:

1.  State the milestone and the precise acceptance criteria.
2.  Inspect existing code before changing it.
3.  Propose a small implementation plan.
4.  Implement the smallest coherent change.
5.  Build and run relevant tests where the environment permits.
6.  Fix errors caused by the change.
7.  Review the diff for accidental changes, secrets, and unrelated
    refactoring.
8.  Update documentation.
9.  Report files changed, commands run, actual results, limitations, and
    the next recommended task.
10. Stop at the requested checkpoint.

Do not generate the entire application in one pass. Do not add
speculative abstractions or dependencies without a concrete need.

## 7. Hardware and development constraints

The development machine has:

-   Intel Core i3-1215U (12th generation).
-   8 GB RAM.
-   Intel UHD integrated graphics.
-   Approximately 477 GB total storage.

Optimise for a modest CPU-only development machine:

-   Prefer lightweight, CPU-compatible models.
-   Avoid training large models locally.
-   Avoid running an Android emulator unless specifically needed; prefer
    a physical Android device with USB debugging.
-   Avoid running multiple heavyweight services simultaneously.
-   Do not download large models or SDK components without explaining
    their size and purpose.
-   Keep inference frequency and image sizes reasonable.
-   Do not claim a tool will run smoothly without testing it on the
    available machine.

## 8. Security, privacy, and payments

These requirements are mandatory:

-   Screen capture must require explicit Android/platform authorisation
    and provide visible user controls.
-   Do not implement silent or unrestricted screen capture.
-   Minimise image retention and transmission.
-   Keep PayPal client secrets exclusively on the backend in environment
    variables or an appropriate secret store.
-   Never commit `.env` files, credentials, tokens, private keys, or
    personal data.
-   Maintain a safe `.gitignore`.
-   Retrieve authoritative prices and merchant information server-side.
-   Use PayPal Sandbox for development.
-   Require explicit user approval in the supported checkout flow.
-   Validate order state before capture and use idempotency protections.
-   Never allow AI to approve or capture payments.
-   Never treat a client-side success screen as proof of payment.
-   Do not create PayPal orders for unrelated merchants without an
    authorised integration.
-   Do not scrape services or bypass access controls or terms.

If credentials are accidentally exposed, stop, report the exposure, and
advise the owner to rotate them. Do not reproduce the secret in logs or
reports.

## 9. Technical accuracy

-   ML Kit Image Labeling returns image-level labels; it does not supply
    object bounding boxes.
-   A correctly positioned blue dot requires actual object-localisation
    coordinates.
-   Object detection, category classification, exact product
    identification, and merchant/listing verification are separate
    tasks.
-   Confidence scores from different models are not automatically
    comparable or calibrated probabilities.
-   A similarity score ranks candidates; it does not prove identity.
-   If evidence is insufficient, return an uncertain/no-match result
    rather than inventing an exact match.
-   The initial local catalogue is demonstration data and must be
    labelled as such.
-   Fastlane availability depends on merchant integration and
    eligibility; never assume it is universally available.

## 10. Git and change management

-   Check `git status` before editing.
-   Never discard uncommitted work.
-   Prefer small, milestone-based commits.
-   Do not force-push, reset, rebase, or delete branches without
    explicit approval.
-   Do not commit secrets, generated build artefacts, local SDK paths,
    or personal files.
-   Summarise the diff before a milestone checkpoint.
-   If multiple conversations are working in the same repository,
    coordinate through commits and written handoff notes rather than
    relying on chat memory.

## 11. First task --- do not skip ahead

The immediate first task is **Milestone 0 and Milestone 1 only**, as
defined in `MASTER_HANDOFF.md`.

1.  Inspect the repository and machine setup.
2.  Confirm whether an Android project already exists.
3.  If needed, create or complete the Kotlin/Jetpack Compose Android
    project.
4.  Implement Android Photo Picker image selection.
5.  Integrate the specified initial ML Kit Image Labeling experiment,
    using compatible dependency versions.
6.  Display the selected image, labels, and confidence values.
7.  If a blue dot is shown at this stage, label it clearly as a
    temporary UI placeholder---not real object localisation or tracking.
8.  Build and run on a physical device if available.
9.  Report exact build/test outcomes and stop.

Do not begin object detection, backend development, PayPal integration,
live screen capture, or the browser extension until the owner reviews
this checkpoint and authorises the next milestone.

## 12. Required completion report

At the end of every task, report:

-   **Milestone/task:**
-   **Files created or modified:**
-   **Dependencies added or changed:**
-   **Build command and result:**
-   **Tests run and actual results:**
-   **What is implemented:**
-   **What remains mocked, untested, or incomplete:**
-   **Known issues and risks:**
-   **Git status / suggested checkpoint:**
-   **Next recommended task:**

Be honest, incremental, and precise. A small verified milestone is
better than a large unverified code dump.
