# DAY 5 — Engineering Report
**PayPal Sandbox Checkout & Canvas Fix**
_Date: 10 November 2026_

---

## Milestone / Task
Day 5 — Add PayPal Sandbox checkout end-to-end and fix the WebGL tainted-canvas error.

---

## Files Created or Modified

### Backend
| File | Action | Description |
|---|---|---|
| `backend/requirements.txt` | Modified | Added `python-dotenv>=1.0` |
| `backend/.env.example` | Created | Safe-to-commit template for Sandbox credentials |
| `backend/paypal_client.py` | Created | Async PayPal REST client: `get_access_token`, `create_order`, `capture_order` |
| `backend/models.py` | Modified | Added `CreateOrderRequest`, `OrderResponse`, `CaptureResponse` Pydantic models |
| `backend/main.py` | Modified | Added `POST /api/orders/create` and `POST /api/orders/{order_id}/capture` endpoints; bumped version to 2.0.0 |
| `backend/tests/test_api.py` | Modified | Added 3 new PayPal tests (no-credential 503, product-not-found 404, live sandbox skip) |

### Frontend
| File | Action | Description |
|---|---|---|
| `frontend/web/src/types/index.ts` | Modified | Added `CheckoutStatus` and `CheckoutState` types |
| `frontend/web/src/services/api.ts` | Modified | Added `createPayPalOrder()` and `capturePayPalOrder()` functions |
| `frontend/web/src/components/ProductMatchCard.tsx` | Modified | Added PayPal Sandbox checkout button with inline feedback states |
| `frontend/web/src/components/UploadZone.tsx` | Modified | Fixed Watch preset URL to local `/samples/watch.jpg`; removed `crossOrigin` from local image thumbnails |
| `frontend/web/src/App.tsx` | Modified | Added `paypalBanner` state; added `useEffect` to handle PayPal redirect return (`?paypal_status=success&token=ORDER_ID`); added dismissible banner UI |

---

## Dependencies Added or Changed
- `python-dotenv>=1.0` — installed into `backend/.venv` via pip.

---

## Build Command and Result

### Backend tests
```
D:\PVCA\backend\.venv\Scripts\python.exe -m pytest tests\test_api.py -v
```
**Result: 16 passed, 1 skipped (live Sandbox test requires real credentials) — 1.52 s**

### Frontend production build
```
node node_modules\vite\bin\vite.js build
```
**Result: ✓ built in 20.54s — 2852 modules transformed, no errors**

---

## What Is Implemented

### PayPal Sandbox Checkout Flow
1. User selects a matched product in the UI.
2. Product card shows a **"Checkout with PayPal Sandbox"** button (PayPal navy `#003087`).
3. Click calls `POST /api/orders/create` → backend validates product exists → calls PayPal `POST /v2/checkout/orders` → returns `{order_id, approval_url}`.
4. Frontend redirects the browser to the PayPal Sandbox approval page.
5. After buyer approves (or cancels), PayPal redirects back to `http://localhost:5173?paypal_status=success&token=ORDER_ID` (or `cancelled`).
6. `App.tsx` `useEffect` detects URL params → calls `POST /api/orders/{order_id}/capture` → shows a dismissible result banner.
7. URL is cleaned with `history.replaceState` (no reload).

### WebGL Tainted Canvas Fix
- All four preset demo images now load from `public/samples/` (local same-origin URLs):
  - Running Shoes → `/samples/shoes.jpg`
  - Ceramic Mug → `/samples/mug.jpg`
  - Leather Backpack → `/samples/backpack.jpg`
  - Minimalist Watch → `/samples/watch.jpg` ← **fixed** (was Unsplash)
- Removed `crossOrigin="anonymous"` from local image thumbnails (not needed; avoids unnecessary CORS preflight for local assets).
- User-uploaded images use `FileReader` → `data:` URL — same-origin, no taint issue.

---

## What Remains Mocked, Untested, or Incomplete

| Item | Status |
|---|---|
| Live PayPal Sandbox order creation | Untested — requires real sandbox credentials in `backend/.env` |
| Live Sandbox capture | Untested — requires credentials + completed buyer approval |
| PayPal buyer login flow | Manual browser test needed with sandbox buyer account |
| Watch image COCO detection | `/samples/watch.jpg` may not be detected as `clock` — COCO-SSD limitation; not in scope today |
| Shoe/watch/backpack recognition gaps | Noted on Day 3 & 4; not in scope today |
| Production deployment | Not in scope; this is a hackathon demo |

---

## Known Issues and Risks

| Issue | Severity | Notes |
|---|---|---|
| Chunk size warning (~2 MB JS) | Low | Expected with TensorFlow.js bundled. Code splitting recommended post-hackathon. |
| `httpx` + `starlette.testclient` deprecation warning | Very Low | Harmless in tests. Upgrade to `httpx2` post-hackathon. |
| PayPal redirect goes to same tab | Low | Chosen for simplicity. Can switch to popup window post-demo. |
| No CSRF protection on order endpoints | Low | Acceptable for demo; add API key or session auth in production. |
| PayPal returns `token` param (not `paypal_order_id`) | Noted | App correctly reads `token` as `order_id`. Documented in code. |

---

## Git Status / Suggested Checkpoint

Uncommitted changes on top of commit `1d5f755` (Day 3):
- `backend/` (all new — Day 4 + 5)
- `docs/DAY4_ENGINEERING_REPORT.md`
- `docs/WEB_TRANSITION_ENGINEERING_REPORT.md`
- `frontend/android/` (network config — Day 4)
- `frontend/web/` (entire web frontend — Day 4 + 5)
- `start_pvca.bat`, `stop_pvca.bat`
- `README.md`, `.gitignore`

> **Please approve before committing.** Suggested commit message:
>
> ```
> feat: Day 5 — PayPal Sandbox checkout & canvas fix
>
> - Add PayPal Sandbox order create + capture endpoints (FastAPI)
> - Add paypal_client.py with async httpx calls to PayPal REST API
> - Add CreateOrderRequest, OrderResponse, CaptureResponse models
> - Add PayPal checkout button to ProductMatchCard
> - Handle PayPal redirect return in App.tsx with dismissible banner
> - Fix WebGL tainted canvas: Watch preset now local /samples/watch.jpg
> - Backend: 16/16 tests pass (1 skipped — requires sandbox credentials)
> - Frontend: production build 2852 modules ✓
> ```

---

## Setup Instructions for PayPal Sandbox

To test real PayPal Sandbox payments:

1. Go to [developer.paypal.com](https://developer.paypal.com) → **Sandbox** → **Apps & Credentials** → **Create App**.
2. Copy the **Client ID** and **Client Secret**.
3. Create `D:\PVCA\backend\.env` from `.env.example`:
   ```
   PAYPAL_CLIENT_ID=AaBb...your_sandbox_id...
   PAYPAL_CLIENT_SECRET=EeFf...your_sandbox_secret...
   PAYPAL_MODE=sandbox
   PAYPAL_BASE_URL=https://api-m.sandbox.paypal.com
   FRONTEND_RETURN_URL=http://localhost:5173
   ```
4. Restart the backend (run `start_pvca.bat` again).
5. Click **Checkout with PayPal Sandbox** on any matched product card.
6. Log in with your PayPal Sandbox **buyer** account to approve.

> **Never commit `backend/.env`** — it is excluded by `.gitignore`.

---

## Next Recommended Task
- Obtain PayPal Sandbox credentials and perform a manual end-to-end checkout test.
- Review and approve the Day 4 + 5 Git commit.
- Optional: Improve COCO-SSD detection label mapping for shoes and watches.
- Optional: Add a PayPal branded SDK popup instead of full redirect.
