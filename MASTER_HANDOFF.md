# PVCA — PayPal Visual Commerce Assistant
## Master Project Manuscript & AI Coding-Agent Handoff

**Project:** PVCA (PayPal Visual Commerce Assistant)  
**Project category:** AI-powered visual commerce and payment orchestration  
**Primary platform:** Android  
**Secondary platform:** Desktop browser extension  
**Payment ecosystem:** PayPal Developer Platform  
**Development environment:** Android Studio, Kotlin, Python  
**Project stage:** Initial implementation / Proof of Concept  
**Document purpose:** Complete project handoff to OpenAI Codex, Google Antigravity, or another AI coding agent.

---

# PART 1 — PROJECT OVERVIEW

## 1.1 The original idea

Social media platforms have become major advertising and product-discovery channels.

Platforms such as YouTube, Instagram, and X expose users to thousands of products through:

- Advertisements.
- Influencer promotions.
- Sponsored videos.
- Product reviews.
- Fashion and lifestyle content.
- Technology demonstrations.
- Images and short-form videos.

However, discovering a product and purchasing it are often two disconnected processes.

A user might see a particular watch in a YouTube video, a pair of shoes in an Instagram reel, or a laptop in a product advertisement.

To purchase the item, the user typically has to:

1. Identify the product.
2. Search for its name.
3. Find a retailer selling it.
4. Verify that the listing corresponds to the product they saw.
5. Navigate to the merchant's website.
6. Complete checkout.

This process introduces friction between product discovery and purchasing.

**Our original intention is to eliminate as much of this friction as possible.**

Instead of asking users to search for products manually, we want to develop an intelligent software accessory that recognises products directly from the content displayed on their screens.

The software should then provide a direct route from visual product recognition to a secure purchasing experience powered by PayPal.

This is not simply another checkout button.

It is an AI-powered visual commerce assistant.

## 1.2 Project vision

Develop a screen-aware visual commerce assistant that identifies products in authorised on-screen content, locates the most relevant verified product listing, and provides users with a convenient PayPal-powered purchasing journey.

The central concept is:

**See a product → Identify it → Verify its listing → Purchase it.**

The experience should feel natural, lightweight, and integrated into the user's existing digital environment.

## 1.3 Proposed identity

Project name:

**PVCA — PayPal Visual Commerce Assistant**

A distinctive part of the product identity is its interactive blue-dot interface.

When the system identifies a sufficiently recognisable product, it displays an animated blue indicator over the product.

The indicator should:

- Appear automatically when a sufficiently reliable product identification is available.
- Be visually distinct from ordinary interface elements.
- Follow the identified product when it moves.
- Be interactive.
- Open a product-information and purchasing interface when selected.

The blue dot is an important part of the product experience and should not be treated as an ordinary notification icon.

Its final animation, shape, colour, and branding remain open to design refinement.

---

# PART 2 — ORIGINAL THOUGHT PROCESS AND PRODUCT REQUIREMENTS

## 2.1 Screen-aware recognition

The original idea was to create software that could access and inspect what the user sees on their screen.

This is different from requiring a user to take a screenshot and manually upload it every time they see an interesting product.

The intended long-term experience is:

- The user grants the necessary permissions.
- The software operates within the permitted screen-capture environment.
- It analyses visual content.
- It identifies recognisable products.
- It displays interactive product indicators.

This should eventually work across multiple environments, including:

- Social media applications.
- Video platforms.
- Browser pages.
- Online advertisements.
- Product demonstration videos.
- Images and other visual content.

**Important privacy requirement:** Screen access must always be explicitly authorised by the user. The application must not silently capture or inspect other applications. It should provide clear controls to start, pause, and stop recognition.

## 2.2 The blue-dot concept

The most distinctive user-interface concept is an animated blue dot.

The proposed workflow:

1. The system analyses an image or video frame.
2. It detects a potential product.
3. It identifies the product or product category.
4. It estimates whether the identification is sufficiently reliable.
5. It displays a blue dot over the detected product.
6. The dot follows the object if it moves.
7. The user selects the dot.
8. The software displays the corresponding product listing.

The original discussion included an initial 70% confidence threshold.

However, this number must be treated as an experimental starting point, not an established universal accuracy standard.

A model reporting 70% confidence that something is a shoe does not necessarily mean it has identified the exact shoe model with 70% accuracy.

Therefore, PVCA must distinguish between:

- Object detection confidence.
- Product-category confidence.
- Exact product identity confidence.
- Listing verification confidence.

These are separate measurements.

The blue dot should only represent a sufficiently reliable identification according to the appropriate criteria.

## 2.3 Automatic product identification

The user's preferred behaviour is automatic recognition.

The system should not require the user to manually select every product before analysis.

However, automatic detection does not mean automatic purchasing.

The software may automatically identify and highlight products, but:

- The user must choose whether to inspect a product.
- The user must approve the purchase.
- The application must never initiate or capture a payment without explicit authorisation.

## 2.4 One best product match

The preferred experience is to display one best-matched product first.

The user does not want an interface cluttered with dozens of similar product listings.

The matching system should prioritise:

1. Exact product identity.
2. Brand.
3. Model or product variant.
4. Product category.
5. Merchant/listing reliability.
6. Price and availability.

If an exact match cannot be established, the system should communicate uncertainty instead of falsely claiming that a similar product is identical.

Alternative products can be introduced later.

---

# PART 3 — CONFIRMED PRODUCT DECISIONS

The following table records decisions made during the project discussions.

| Area | Decision |
|---|---|
| Core concept | Screen-aware visual commerce assistant |
| Project name | PVCA |
| Product indicator | Animated blue dot |
| Product identification | Automatic when sufficiently reliable |
| Dot behaviour | Track the detected product |
| Platforms | Mobile and desktop |
| Initial implementation | Android first |
| Desktop implementation | Browser extension later |
| Product matching | Hybrid retrieval |
| Primary retrieval source | Internal verified catalogue |
| Secondary retrieval source | External product search |
| Product display | One best match first |
| Payment provider | PayPal |
| Fastlane | Use where merchant integration and eligibility support it |
| Payment alternative | Standard PayPal checkout or merchant checkout |
| Development experience | Beginner |
| Hardware | CPU only |
| Initial development timeframe | Less than one week |
| First implementation priority | A working Android proof of concept |

## 3.1 Decisions that remain open

The following have not been conclusively decided:

- Final application name and logo.
- Final blue-dot animation.
- Exact confidence thresholds.
- Object-detection model.
- Product embedding model.
- External product-search provider.
- Production database.
- Cloud hosting provider.
- Final desktop implementation technology.
- Commercial merchant partnerships.
- Production-level payment integration.

The coding agent must not silently make irreversible architectural decisions about these areas.

It should recommend reasonable MVP defaults and document them.

---

# PART 4 — SYSTEM ARCHITECTURE

## 4.1 Overall architecture

PVCA should use a modular hybrid architecture.

The main subsystems are:

1. Client applications.
2. Visual intelligence.
3. Product intelligence.
4. Product catalogue.
5. Payment orchestration.
6. Data and analytics.
7. Administration and monitoring.

### High-level system flow

User's screen or selected image  
↓  
Image acquisition  
↓  
Object detection  
↓  
Object localisation  
↓  
OCR and visual feature extraction  
↓  
Product matching engine  
↓  
Internal verified catalogue  
↓  
External product-search fallback  
↓  
Best product candidate  
↓  
Product and merchant verification  
↓  
Interactive blue-dot interface  
↓  
User selects product  
↓  
PayPal checkout  
↓  
User approval  
↓  
Payment capture and confirmation

## 4.2 Client applications

### A. Android application — Primary

The first implementation must be an Android application.

Recommended technology:

- Kotlin.
- Jetpack Compose.
- Android Studio.
- Android Photo Picker.
- Google ML Kit.
- Kotlin Coroutines.

Initial responsibilities:

- Allow users to select an image.
- Process the selected image.
- Display detected objects or labels.
- Display the blue-dot interface.
- Allow users to select an identified product.
- Display matching product information.

Later responsibilities:

- Camera/video input.
- Live video recognition.
- Screen capture through Android MediaProjection.
- Floating overlay interface.
- Tracking objects across frames.
- Backend communication.
- PayPal checkout integration.

The first prototype must not attempt unrestricted access to the user's screen.

Android MediaProjection requires explicit user authorisation. It should be introduced only after the initial recognition workflow works reliably.

### B. Desktop application — Secondary

The proposed desktop implementation is a browser extension.

Potential technologies:

- JavaScript or TypeScript.
- Chrome Extension Manifest V3.
- HTML/CSS.
- Browser content scripts.
- Extension background service worker.

Potential responsibilities:

- Analyse authorised visual content on browser pages.
- Display product indicators over detected items.
- Track product positions within the page.
- Communicate with the PVCA backend.
- Open the product-matching interface.

Browser restrictions, cross-origin content, protected media, and video-capture limitations must be considered.

The desktop extension must not be assumed to have unrestricted access to every application or every browser-rendered video frame.

---

# PART 5 — AI AND VISUAL INTELLIGENCE

## 5.1 Recognition pipeline

The visual intelligence subsystem should contain separate stages.

### Stage 1: Image acquisition

Acquire an image or frame from an authorised source.

Initial implementation:

Android Photo Picker.

Later implementations:

- Camera.
- Video frames.
- Authorised screen capture.
- Browser-page imagery.

### Stage 2: Object detection

Object detection determines:

- What objects are present.
- Where the objects are located.
- Their bounding-box coordinates.
- Their detection confidence.

For example:

A video frame may contain a shoe, a person, and a table.

The detector should identify the shoe's location independently of the other objects.

**Important technical distinction:** Image classification and object detection are not the same thing.

Google ML Kit Image Labeling can produce image-level labels, but it does not provide the bounding boxes required to position the blue dot over an actual object.

Therefore, Image Labeling is suitable for the first recognition experiment, but actual object positioning requires an object-detection implementation.

### Stage 3: Object tracking

Tracking should maintain an object's identity and approximate position across successive video frames.

The blue dot should not jump randomly between objects.

The tracking subsystem should:

- Associate detections across frames.
- Maintain object identifiers.
- Update bounding-box coordinates.
- Handle temporary occlusion.
- Remove indicators when objects disappear.
- Avoid unnecessary repeated inference.

Tracking can be introduced after static-image object detection.

### Stage 4: OCR

Optical Character Recognition should identify visible textual information.

Useful information includes:

- Brand names.
- Product model numbers.
- Product names.
- Labels.
- Visible manufacturer identifiers.

OCR should support visual matching, not independently establish product identity.

### Stage 5: Visual embeddings

An image-embedding model can convert a product image or detected crop into a numerical vector.

These embeddings can be used to compare the detected product against catalogue images.

Potential future approaches include:

- CLIP-style image embeddings.
- Mobile-compatible image embedding models.
- Vector similarity search.

The implementation should favour lightweight CPU-compatible models initially.

### Stage 6: Product identification

The system combines:

- Object category.
- Visual similarity.
- OCR information.
- Brand.
- Model information.
- Catalogue metadata.

The final output should identify a candidate product only when sufficient evidence exists.

## 5.2 Confidence and uncertainty

The original 70% threshold is an initial design idea.

Do not apply a single threshold to every stage.

A recommended decision structure:

| Signal | Purpose |
|---|---|
| Detection confidence | Is an object present? |
| Category confidence | What kind of object is it? |
| Visual similarity | Does it resemble the catalogue image? |
| Brand evidence | Does the brand match? |
| Model evidence | Does the model or variant match? |
| Listing verification | Is the product listing trustworthy? |

The application should support three outcomes:

1. High-confidence match: display the interactive product dot.
2. Insufficient exact identity: optionally display a category-level discovery result.
3. Low confidence: do not claim a product match.

A false exact match is worse than admitting uncertainty.

---

# PART 6 — HYBRID PRODUCT RETRIEVAL

## 6.1 Why hybrid retrieval?

The project previously considered whether PVCA should use only an internal catalogue or rely entirely on external product search.

The chosen direction is a hybrid approach.

The internal catalogue provides reliable, controlled data.

External search expands product coverage.

Combining them provides a more practical architecture than relying exclusively on either source.

## 6.2 Path A — Internal verified catalogue

The internal catalogue is the primary retrieval source.

For the MVP, create a catalogue of approximately 50–100 demonstration products.

Suggested product fields:

| Field | Description |
|---|---|
| product_id | Unique internal identifier |
| brand | Product brand |
| product_name | Product name |
| category | Product category |
| model | Product model |
| sku | Stock keeping unit |
| image_url | Product image |
| price | Current demonstration price |
| currency | Price currency |
| merchant_id | Associated merchant |
| listing_status | Listing verification status |
| purchase_url | Permitted purchase destination |

The catalogue should initially be small enough to manage manually.

It can later be expanded through approved product feeds or merchant integrations.

## 6.3 Matching algorithm

A proposed initial scoring model:

\[
S = 0.40V + 0.25B + 0.20T + 0.10C + 0.05L
\]

Where:

- \(V\): visual similarity.
- \(B\): brand match.
- \(T\): model and text match.
- \(C\): category match.
- \(L\): listing quality.

These weights are experimental starting hypotheses.

They must be calibrated against a labelled test dataset.

The score is a ranking mechanism, not proof of identity.

The system must also consider conflicting evidence.

For example, an excellent visual similarity score should not override a clearly different model number.

## 6.4 Path B — External product retrieval

If the internal catalogue does not provide a sufficiently reliable match, the system may use an external product-search adapter.

Potential search inputs:

- OCR-extracted brand.
- Product category.
- Product description.
- Model number.
- Visual-search query, if supported.

The external adapter should return structured product candidates.

Each candidate must be validated for:

- Product identity.
- Merchant identity.
- Listing availability.
- Price.
- Currency.
- Purchase destination.
- Listing reliability.

The external-search integration should be provider-independent.

Do not hardwire the entire application to one external shopping platform.

Also, do not scrape websites in violation of their terms or bypass access restrictions.

## 6.5 Product-matching output

The backend should return a structured result.

Example:

```json
{
  "match_status": "verified",
  "product_id": "PVCA-0001",
  "product_name": "Example Running Shoe",
  "brand": "Example Brand",
  "category": "Footwear",
  "price": 4999,
  "currency": "INR",
  "match_score": 0.91,
  "source": "internal_catalogue",
  "merchant_verified": true
}
```

This is an illustrative response, not an actual product listing.

---

# PART 7 — PAYPAL PAYMENT INTEGRATION

## 7.1 Why PayPal?

PayPal is not merely an additional payment button in this project.

It is the payment infrastructure that connects product discovery with a trusted purchasing experience.

The AI identifies and retrieves the product.

The backend verifies the product and merchant information.

PayPal handles the payment workflow.

The user remains in control of the transaction.

## 7.2 Payment architecture

Recommended payment sequence:

1. User selects a product.
2. Backend retrieves the authoritative product record.
3. Backend validates the merchant, price, currency, and purchase destination.
4. Backend creates a PayPal order for an appropriately integrated demo merchant.
5. Client receives the necessary checkout information.
6. User approves the payment through the PayPal-supported checkout flow.
7. Backend verifies the order.
8. Backend captures the payment through the appropriate PayPal API.
9. Backend records the transaction result.
10. User receives confirmation.

The AI must never initiate payment capture.

## 7.3 PayPal Orders API

The MVP should investigate and use the PayPal Orders v2 API.

The backend should manage:

- Order creation.
- Order identifiers.
- Approval flow.
- Capture.
- Transaction status.
- Error handling.
- Idempotency.

PayPal credentials and secret keys must remain on the backend.

Never embed private PayPal credentials in:

- Android application code.
- Browser extension code.
- Public repositories.
- Client-side JavaScript.

Use PayPal Sandbox credentials during development.

## 7.4 Fastlane

Fastlane was part of the original project inspiration.

The idea is to reduce friction in the final checkout experience.

However, Fastlane should not be treated as a universal payment API for every merchant.

Its availability depends on the merchant's integration and eligibility.

Recommended approach:

- Use Fastlane where supported by the merchant integration.
- Use standard PayPal checkout where appropriate.
- Redirect to the merchant's checkout when PVCA does not have an authorised payment integration.

PVCA must not create arbitrary PayPal orders on behalf of unrelated merchants.

For the hackathon prototype, use a controlled demonstration merchant integrated with PayPal Sandbox.

## 7.5 Security requirements

Essential requirements:

- Backend-only secret management.
- Server-side price verification.
- Server-side merchant verification.
- Order ownership and status validation.
- Idempotency for order creation and capture.
- Payment-state validation.
- Secure API communication.
- No automatic payment capture without approval.
- No trust in AI-generated prices.
- No trust in client-supplied merchant information.

The application must distinguish between:

- Product identification.
- Product verification.
- Order creation.
- Payment approval.
- Payment capture.

These are separate operations.

---

# PART 8 — PROPOSED TECHNICAL STACK

The following stack is recommended for the first implementation.

| Component | Recommended technology |
|---|---|
| Android application | Kotlin |
| Android UI | Jetpack Compose |
| Development environment | Android Studio |
| Initial image selection | Android Photo Picker |
| Initial image analysis | Google ML Kit Image Labeling |
| Object detection | ML Kit Object Detection or another suitable detector |
| OCR | Google ML Kit Text Recognition |
| Backend | Python FastAPI |
| Backend communication | REST API |
| Initial database | SQLite |
| Future database | PostgreSQL |
| Visual embeddings | Lightweight compatible image model |
| Similarity search | Cosine similarity |
| Future vector database | Optional, depending on scale |
| Desktop extension | TypeScript/JavaScript |
| Desktop platform | Chrome Extension Manifest V3 |
| Payment | PayPal Orders v2 |
| Payment testing | PayPal Sandbox |
| Version control | Git and GitHub |

This is a proposed implementation stack, not a requirement to introduce every technology immediately.

The coding agent should avoid unnecessary complexity.

A working modular MVP is more valuable than an incomplete enterprise architecture.

---

# PART 9 — INITIAL PROJECT STRUCTURE

Suggested repository:

```text
PVCA/
│
├── frontend/
│   ├── android/
│   │   ├── app/
│   │   └── README.md
│   │
│   ├── web-dashboard/
│   │
│   └── chrome-extension/
│       ├── manifest.json
│       ├── background.js
│       ├── content.js
│       ├── overlay.js
│       └── popup/
│
├── backend/
│   ├── main.py
│   │
│   ├── routes/
│   │   ├── detection.py
│   │   ├── products.py
│   │   └── payments.py
│   │
│   ├── services/
│   │   ├── detection_service.py
│   │   ├── tracking_service.py
│   │   ├── matching_service.py
│   │   ├── catalogue_service.py
│   │   ├── external_search.py
│   │   └── paypal_service.py
│   │
│   ├── models/
│   ├── database/
│   ├── config/
│   └── tests/
│
├── ai/
│   ├── object_detection/
│   ├── tracking/
│   ├── ocr/
│   ├── embeddings/
│   └── evaluation/
│
├── data/
│   └── demo_catalogue/
│
├── docs/
│   ├── architecture.md
│   ├── api.md
│   ├── security.md
│   ├── testing.md
│   └── milestones.md
│
├── tests/
│
├── README.md
└── .gitignore
```

The actual repository may differ slightly if the coding agent chooses a cleaner architecture.

Do not create empty folders or unnecessary abstractions merely to reproduce this tree.

---

# PART 10 — PROPOSED BACKEND API

The following endpoints provide an initial API design.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/detect` | Detect objects in an image |
| POST | `/api/track` | Update tracking information |
| POST | `/api/products/match` | Find the best product match |
| GET | `/api/products/{id}` | Retrieve product information |
| POST | `/api/products/search` | Search the product catalogue |
| POST | `/api/checkout/create` | Create a PayPal order |
| POST | `/api/checkout/capture` | Capture an approved PayPal order |
| GET | `/api/health` | Check backend availability |

The endpoints are proposed interfaces. Their exact schemas should be finalised before implementation.

## 10.1 Example detection response

```json
{
  "detections": [
    {
      "object_id": "obj-001",
      "category": "shoe",
      "confidence": 0.94,
      "bounding_box": {
        "x": 0.22,
        "y": 0.31,
        "width": 0.38,
        "height": 0.29
      }
    }
  ]
}
```

Coordinates should use a documented normalised coordinate system or clearly specified pixel coordinates.

## 10.2 Example product matching request

```json
{
  "object_id": "obj-001",
  "category": "shoe",
  "brand_text": "Example Brand",
  "model_text": "Example Model",
  "visual_embedding": []
}
```

The visual embedding field is illustrative. It should not be transmitted as a placeholder if no embedding model has been implemented.

## 10.3 Example checkout request

```json
{
  "product_id": "PVCA-0001"
}
```

The backend must retrieve the authoritative price and merchant information itself.

The client must not determine the transaction amount.

---

# PART 11 — HACKATHON REQUIREMENTS

The project was developed in the context of a PayPal hackathon.

The requirements identified from the hackathon material include:

1. Meaningful integration with at least one PayPal technology, API, SDK, or developer capability.
2. Meaningful incorporation of AI.
3. A functioning prototype or proof of concept.
4. Sufficient project documentation for evaluation.
5. Compliance with the official hackathon rules.

The sponsor tools presented in the hackathon material included:

- AG Grid.
- APIMatic.
- Astropods.
- Bryntum.
- Channel3.
- Elastic.
- KERNEL.
- Postman.
- Render.
- Zapier.

These are optional technologies, not mandatory project dependencies.

The implementation should only use them if they genuinely improve the product.

## 11.1 Demonstration objective

The final prototype should demonstrate a complete, understandable user journey:

1. Open PVCA.
2. Select an image containing a product.
3. Detect the product.
4. Display its location.
5. Display the animated blue dot.
6. Select the dot.
7. Retrieve a matching catalogue product.
8. Display its details.
9. Start PayPal Sandbox checkout.
10. Approve the payment.
11. Display a successful transaction result.

The prototype should clearly distinguish implemented capabilities from planned features.

---

# PART 12 — DEVELOPMENT MILESTONES

## Development philosophy

The developer has beginner-level programming experience and access to CPU-only hardware.

The initial development target was less than one week.

Therefore, development must be incremental.

The coding agent should prioritise a demonstrable working application over advanced features.

## MILESTONE 0 — Environment setup

**Status: Not confirmed complete**

Tasks:

- Install Android Studio.
- Install the required Android SDK.
- Create a Kotlin Android project.
- Configure Jetpack Compose.
- Configure minimum supported Android SDK.
- Set up Git.
- Create the project repository.

Acceptance criteria:

- The empty Android application builds.
- The application launches on a device or emulator.
- Git is configured.

## MILESTONE 1 — Image selection and initial AI analysis

**Status: Code previously supplied; execution not confirmed**

The initial implementation was designed around:

- Kotlin.
- Jetpack Compose.
- Android Photo Picker.
- Google ML Kit Image Labeling.

Dependency previously proposed:

```kotlin
implementation("com.google.mlkit:image-labeling:17.0.9")
```

The Android project should use a compatible minimum SDK.

Initial functionality:

1. User selects an image.
2. The application loads the image.
3. ML Kit analyses it.
4. Image labels and confidence values are displayed.
5. A temporary blue-dot UI element is shown.

The first code prototype used package:

```text
com.pvca.app
```

Important limitation:

ML Kit Image Labeling returns image-level labels, not object bounding boxes.

The initial blue dot was a temporary interface placeholder positioned at the image centre.

It did not represent actual object tracking or product localisation.

Acceptance criteria:

- Image selection works.
- Image loading works.
- ML Kit returns labels.
- Results appear in the interface.
- No application crash during normal image processing.

## MILESTONE 2 — Actual object detection

**Priority: High**

Replace or supplement image-level classification with object detection.

Requirements:

- Detect objects.
- Retrieve bounding boxes.
- Retrieve detection confidence.
- Draw the blue indicator at the detected object's position.
- Support more than one detection internally.
- Select a single suitable product candidate for the first user experience.

Acceptance criteria:

The blue indicator must be positioned using actual detector coordinates, not an arbitrary image location.

## MILESTONE 3 — Blue-dot interface

Requirements:

- Animated blue dot.
- Clear visual indication of an identifiable product.
- Interactive selection.
- Stable positioning.
- Product-information popup or panel.
- Graceful disappearance when the detection becomes invalid.

Future enhancements:

- Glow effect.
- Beaming animation.
- Tracking animation.
- Multiple product indicators.
- Custom PVCA visual identity.

## MILESTONE 4 — Product catalogue

Requirements:

- Create a local demonstration catalogue.
- Add approximately 50–100 products.
- Include product images.
- Include product names and brands.
- Include models and categories.
- Include demonstration prices.
- Include merchant information.
- Include listing status.

Initially, local JSON or SQLite is sufficient.

Acceptance criteria:

A detected product can be matched against catalogue entries and a best candidate can be displayed.

## MILESTONE 5 — Product matching engine

Requirements:

- Visual similarity.
- OCR-based brand recognition.
- Model-text comparison.
- Category matching.
- Candidate ranking.
- Confidence thresholds.
- Uncertainty handling.

Acceptance criteria:

The system can identify a known catalogue product from a representative image and reject clearly incorrect matches.

## MILESTONE 6 — Backend development

Introduce FastAPI.

Requirements:

- Product endpoints.
- Detection endpoints where required.
- Catalogue services.
- Matching services.
- Configuration management.
- Error handling.
- Logging.
- API documentation.

The Android client should communicate with the backend only when necessary.

Do not move all image processing to the server merely for architectural consistency. On-device inference may be more practical for the initial CPU-constrained implementation.

## MILESTONE 7 — PayPal Sandbox integration

Requirements:

- Create a controlled demo merchant integration.
- Configure Sandbox credentials.
- Create orders through the backend.
- Implement the appropriate approval flow.
- Capture approved orders.
- Validate payment status.
- Record transaction results.

Acceptance criteria:

A user can complete a Sandbox payment for a catalogue product.

## MILESTONE 8 — Integration and demonstration

Requirements:

- Connect detection, matching, and checkout.
- Handle failed matches.
- Handle failed checkout.
- Handle network errors.
- Handle invalid products.
- Provide a complete demonstration flow.

Acceptance criteria:

The entire MVP can be demonstrated from image selection to successful Sandbox payment.

## MILESTONE 9 — Desktop browser extension

This is a secondary platform.

Requirements:

- Chrome Extension Manifest V3.
- Browser-page visual processing.
- Product indicator overlay.
- Backend integration.
- Product details.
- Checkout handoff.

This should begin after the Android MVP has a stable architecture.

## MILESTONE 10 — Live screen recognition

This is a later-stage feature.

Android:

- MediaProjection.
- Explicit screen-capture authorisation.
- Frame acquisition.
- Frame processing.
- Object tracking.
- Floating overlay.

Desktop:

- Browser permissions.
- Content-script integration.
- Page and video limitations.
- Overlay positioning.

iOS should be assessed separately because its operating-system restrictions differ.

---

# PART 13 — BACKLOG AND PRIORITY MATRIX

| Feature | Priority | Stage |
|---|---|---|
| Android application setup | P0 | Initial |
| Image selection | P0 | Initial |
| Basic AI recognition | P0 | Initial |
| Object detection | P0 | Initial |
| Bounding-box rendering | P0 | Initial |
| Blue-dot interaction | P0 | Initial |
| Local product catalogue | P0 | MVP |
| Product matching | P0 | MVP |
| PayPal Sandbox | P0 | MVP |
| Error handling | P0 | MVP |
| Documentation | P0 | MVP |
| OCR | P1 | MVP enhancement |
| Visual embeddings | P1 | MVP enhancement |
| External product search | P1 | Later MVP |
| Animated tracking | P1 | Later MVP |
| Backend integration | P1 | MVP |
| Transaction history | P1 | Later |
| Product alternatives | P2 | Future |
| Chrome extension | P2 | Future |
| Live Android screen capture | P2 | Future |
| Advanced analytics | P2 | Future |
| Merchant onboarding | P2 | Future |
| Production deployment | P2 | Future |
| Multi-merchant ecosystem | P3 | Long-term |
| iOS implementation | P3 | Long-term |

Priority meanings:

- P0: Essential to the first demonstrable prototype.
- P1: Important enhancement.
- P2: Secondary expansion.
- P3: Long-term development.

---

# PART 14 — TESTING AND EVALUATION

Testing must cover both technical functionality and product reliability.

## 14.1 Object detection tests

Use a small labelled image dataset containing:

- Clear product images.
- Products at different angles.
- Partially visible products.
- Multiple objects.
- Cluttered backgrounds.
- Poor lighting.
- Low-resolution images.

Evaluate:

- Detection accuracy.
- Bounding-box quality.
- Inference time.
- False detections.
- Missed detections.

## 14.2 Product matching tests

Test:

- Exact product images.
- Similar products from the same brand.
- Similar products from different brands.
- Different models in the same category.
- Images with visible model numbers.
- Images without visible text.
- Products absent from the catalogue.

Measure:

- Top-1 matching accuracy.
- False exact-match rate.
- Match coverage.
- Uncertain-result frequency.

The false exact-match rate is particularly important.

PVCA must not confidently recommend the wrong product simply to avoid displaying an uncertain result.

## 14.3 UI tests

Verify:

- Blue-dot positioning.
- Indicator stability.
- Selection behaviour.
- Popup behaviour.
- Screen-size compatibility.
- Loading states.
- Empty states.
- Error states.

## 14.4 Payment tests

Use PayPal Sandbox.

Test:

- Successful approval and capture.
- User cancellation.
- Invalid product.
- Changed price.
- Duplicate requests.
- Network failure.
- Invalid order state.
- Repeated capture attempts.

No real payment processing is necessary for the initial hackathon demonstration.

---

# PART 15 — PRIVACY, SECURITY AND ETHICAL REQUIREMENTS

Privacy and security must be part of the architecture, not features added at the end.

## 15.1 Screen privacy

- Obtain explicit permission before screen capture.
- Explain what is being analysed.
- Provide a visible active state.
- Provide a pause/stop control.
- Do not secretly capture screens.
- Avoid unnecessary retention of screenshots or video frames.
- Do not transmit personal screen content unnecessarily.

## 15.2 Payment security

- Keep secrets on the backend.
- Validate products and prices server-side.
- Validate merchant identity.
- Validate order state.
- Use idempotency.
- Verify payment results.
- Never allow the AI to approve or capture a payment.
- Never treat a client-side success message as proof of payment.

## 15.3 Product accuracy

- Avoid claiming exact identification without evidence.
- Do not confuse product category with exact product identity.
- Display uncertainty.
- Validate merchant listings.
- Avoid misleading purchase destinations.

---

# PART 16 — TECHNICAL RISKS AND LIMITATIONS

| Risk | Impact | Mitigation |
|---|---|---|
| Similar-looking products | Incorrect matching | Combine visual, OCR and catalogue evidence |
| Low-resolution imagery | Poor recognition | Confidence-based rejection |
| Product partially hidden | Missed detection | Improve model and test occlusion |
| CPU limitations | Slow processing | Use lightweight models and frame throttling |
| Tracking instability | Dot jumps between objects | Add temporal tracking and smoothing |
| External search limitations | Incomplete catalogue | Use an adapter-based retrieval architecture |
| Merchant restrictions | Checkout unavailable | Use authorised merchant integrations |
| Screen-capture restrictions | Limited platform access | Respect operating-system permission models |
| Payment manipulation | Financial loss | Backend validation and PayPal verification |
| False confidence | Misleading user experience | Calibrate confidence and support abstention |

## 16.1 Important architectural distinction

The system should not be built around the assumption that every social media platform allows unrestricted screen inspection or direct checkout integration.

PVCA's core product-discovery functionality can work with authorised images and browser content without requiring special partnerships with every social platform.

Direct purchasing through third-party merchants may require their authorisation or supported integration.

The first prototype should therefore use a controlled demonstration catalogue and an integrated demo merchant.

---

# PART 17 — DEVELOPMENT CONSTRAINTS

The original development constraints are:

- Beginner-level programming experience.
- CPU-only hardware.
- Less than one week for the initial prototype.
- Android implementation first.
- Desktop extension second.

The coding agent must account for these constraints.

Recommended practices:

1. Keep the first implementation small.
2. Prefer on-device lightweight inference where practical.
3. Avoid training large AI models from scratch.
4. Use existing pretrained models.
5. Avoid unnecessary cloud infrastructure.
6. Build and test after each milestone.
7. Explain significant code changes.
8. Do not generate thousands of lines of code in one step.
9. Keep configuration simple.
10. Maintain clear documentation.

---

# PART 18 — INSTRUCTIONS TO THE CODING AGENT

## 18.1 Your role

You are the primary AI software engineering agent responsible for helping implement PVCA.

The project owner is a beginner programmer.

You should act as a senior software engineer and technical mentor.

Your responsibility is not simply to generate code.

You must produce a working, understandable, maintainable prototype.

## 18.2 First actions

Before implementing anything:

1. Inspect the existing repository.
2. Identify whether an Android project already exists.
3. Inspect its Gradle configuration.
4. Identify existing dependencies.
5. Check the installed Android SDK and build environment.
6. Read the complete project manuscript.
7. Create a concise implementation plan.
8. Identify missing prerequisites.

Do not overwrite existing work without checking it.

Do not assume that the previously supplied code has already been executed successfully.

## 18.3 Immediate implementation target

Start with Milestone 0 and Milestone 1.

The first objective is:

**Build a working Android application that allows the user to select an image and perform basic on-device AI image analysis.**

After that:

- Implement real object detection.
- Obtain bounding boxes.
- Position the blue dot correctly.
- Make the dot interactive.
- Connect it to a local demonstration catalogue.

Do not start with live screen capture, a desktop extension, or payment processing.

Those are subsequent stages.

## 18.4 Coding standards

- Use Kotlin for Android.
- Use Jetpack Compose for the Android UI.
- Use Python FastAPI for the backend when needed.
- Separate UI, business logic, and data access.
- Use clear names.
- Handle errors.
- Avoid hardcoded credentials.
- Avoid unnecessary dependencies.
- Keep code compatible with the target Android SDK.
- Write basic tests.
- Maintain a clear README.

## 18.5 Working methodology

For every milestone:

1. Explain the objective.
2. Inspect the current implementation.
3. Implement only the necessary changes.
4. Build the project.
5. Run available tests.
6. Fix compilation errors.
7. Explain what was implemented.
8. Identify any limitations.
9. Update the project documentation.
10. Propose the next milestone.

Do not claim a feature is working unless it has actually been built and tested.

If the build cannot be executed in the current environment, clearly state that it remains unverified.

## 18.6 Important instructions

Do not:

- Replace object detection with image classification and claim that bounding boxes exist.
- Position the blue dot arbitrarily and claim it tracks a product.
- Allow AI to determine payment amounts.
- Store PayPal secrets in the client.
- Assume Fastlane is universally available.
- Scrape external shopping websites without checking their permitted access.
- Implement unrestricted screen capture.
- Add unnecessary infrastructure before the MVP works.
- Invent test results.

Do:

- Preserve the original PVCA vision.
- Keep the architecture modular.
- Prioritise the Android MVP.
- Use a verified local catalogue first.
- Build a safe PayPal Sandbox flow.
- Document technical decisions.
- Explain implementation choices to the project owner.

---

# PART 19 — THE OTHER ORIGINAL PAYPAL IDEAS

These ideas were part of the broader PayPal project discussion but are deliberately outside PVCA's current implementation scope.

They should be preserved as separate potential project directions.

## Idea 2 — Multi-recipient supplier payments

Original concept:

A food company may work with a large farm or multiple independent farms.

Instead of manually calculating and initiating each payment, the company could use predefined payment pathways.

The workflow would resemble a GPay QR-based payment experience.

Potential functionality:

- Registered supplier profiles.
- Verified bank/payment destinations.
- Predefined payment instructions.
- Bulk payment initiation.
- Automated amount allocation.
- Payment reconciliation.
- Transaction reporting.

Potential benefit:

Reduce accounting effort and administrative time when paying multiple suppliers.

This idea requires investigation of PayPal's applicable payout and disbursement capabilities, eligibility, and geographic restrictions.

## Idea 3 — Cross-border business payments

Original concept:

An American multinational company may purchase raw materials from manufacturers in India.

The US company pays in USD, while the Indian manufacturer needs INR.

A potential system could simplify:

- Currency conversion.
- Cross-border settlement.
- Exchange-rate transparency.
- Transaction fees.
- Supplier payment confirmation.

The architecture must distinguish between displaying an exchange-rate estimate, converting currency, and actually settling funds.

These are different financial operations.

## Idea 4 — Worker payroll and wage access

Original concept:

Employees or factory workers receive their wages through PayPal.

They could then:

- Use their PayPal balance for eligible purchases.
- Transfer eligible funds to a supported bank account.
- Access transaction records.
- Receive wage notifications.

Important areas requiring investigation:

- Payroll compliance.
- Employee identity verification.
- Payout eligibility.
- Local banking infrastructure.
- Currency conversion.
- Worker access and financial inclusion.

## Idea 5 — Improving payment infrastructure and transfer economics

Original concern:

PayPal balances and bank deposits are not necessarily equivalent.

The user questioned whether PayPal's wallet-style infrastructure is appropriate for handling very large payment volumes and whether transfer costs could be reduced.

The original objectives included:

- Increasing practical transaction capacity.
- Reducing transfer fees.
- Improving infrastructure scalability.
- Maintaining cybersecurity.
- Improving interbank transfer efficiency.

The previously discussed technical direction was a combined infrastructure approach, with all these outcomes considered important.

However, this is a substantial financial infrastructure research problem.

It must begin with an evidence-based analysis of PayPal's actual existing architecture and limitations.

Do not assume that PayPal has a simple fixed balance or transaction-capacity limitation.

Do not claim that the project can replace bank settlement infrastructure without a credible technical and regulatory model.

**Scope rule:** Do not merge Ideas 2–5 into PVCA. Preserve them as independent future concepts.

---

# PART 20 — FINAL PROJECT DEFINITION

PVCA is an AI-powered visual commerce assistant designed to reduce the gap between seeing a product and purchasing it.

It combines:

- Authorised screen or image acquisition.
- AI-based object detection.
- Object tracking.
- OCR.
- Visual product matching.
- Hybrid catalogue retrieval.
- Merchant and listing verification.
- Interactive blue-dot product discovery.
- PayPal-powered checkout.

Its defining user experience is the ability to recognise a product visually and make it actionable through a lightweight interactive indicator.

Its defining technical challenge is not merely detecting objects.

It is accurately connecting an object visible in an image or video to the correct purchasable product listing.

Its defining payment principle is that AI assists product discovery, while verified backend services and PayPal handle the payment workflow.

Its first practical implementation is an Android application that begins with static image selection and gradually develops into a more sophisticated visual commerce system.

**The immediate objective is a working Android proof of concept, not a fully developed commercial platform.**

---

# PART 21 — FIRST TASK FOR CODEX / ANTIGRAVITY

Begin by inspecting the current development environment and repository.

Then implement:

### Task 1: Android project setup

Create or inspect the Android Studio Kotlin project.

### Task 2: Image selection

Implement Android Photo Picker.

### Task 3: AI image analysis

Integrate Google ML Kit Image Labeling.

### Task 4: Basic interface

Display:

- Selected image.
- Detected labels.
- Confidence values.
- Temporary blue-dot indicator.

### Task 5: Verification

Build and run the application.

Confirm that image selection and AI analysis work.

### Task 6: Report

Provide:

- Files created.
- Dependencies installed.
- Build result.
- Tests performed.
- Known limitations.
- Next recommended task.

**Stop after this first working checkpoint and report the result before expanding the implementation.**

The project owner should be able to understand what has been built before moving to the next milestone.

---

# END OF MASTER HANDOFF MANUSCRIPT

**Project identity:** PVCA — PayPal Visual Commerce Assistant  
**Current development priority:** Android-first MVP  
**Immediate milestone:** Image selection + basic on-device AI recognition  
**Long-term vision:** A cross-platform, AI-powered visual product discovery and PayPal checkout ecosystem.