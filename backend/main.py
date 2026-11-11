"""
PVCA FastAPI backend — Day 5
Endpoints:
  GET  /api/health
  GET  /api/products
  GET  /api/products/search?q=...
  GET  /api/products/{id}
  POST /api/products/match
  POST /api/orders/create
  POST /api/orders/{order_id}/capture
"""
from __future__ import annotations

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse

import catalogue as cat
import matching as match_svc
import paypal_client as pp
from models import (
    MatchRequest,
    MatchResponse,
    ProductItem,
    CreateOrderRequest,
    OrderResponse,
    CaptureResponse,
)

app = FastAPI(
    title="PVCA Backend API",
    description="PayPal Visual Commerce Assistant — Day 5 backend",
    version="2.0.0",
)

# Allow requests from any origin (useful for Android development / testing)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)


# ---------------------------------------------------------------------------
# Health
# ---------------------------------------------------------------------------

@app.get("/api/health")
def health():
    return {
        "status": "ok",
        "version": "2.0.0",
        "paypal_configured": pp.credentials_available(),
    }


# ---------------------------------------------------------------------------
# Products
# ---------------------------------------------------------------------------

@app.get("/api/products", response_model=list[ProductItem])
def list_products():
    """Return all products from the demo catalogue."""
    return cat.load_catalogue()


@app.get("/api/products/search", response_model=list[ProductItem])
def search_products(q: str = Query(default="", description="Keyword search query")):
    """Keyword search across title, brand, category, keywords, description."""
    return cat.search_products(q)


@app.get("/api/products/{product_id}", response_model=ProductItem)
def get_product(product_id: str):
    """Return a single product by ID, or 404."""
    product = cat.get_product_by_id(product_id)
    if product is None:
        raise HTTPException(status_code=404, detail=f"Product '{product_id}' not found.")
    return product


# ---------------------------------------------------------------------------
# Matching
# ---------------------------------------------------------------------------

@app.post("/api/products/match", response_model=MatchResponse)
def match_product(request: MatchRequest):
    """
    Rank catalogue products against a detected object.
    Returns the best candidate with confidence tier, or a no-match response.
    """
    return match_svc.find_best_match(
        detected_category=request.detected_category,
        crop_labels=request.crop_labels,
        scene_labels=request.scene_labels,
        user_query=request.user_query,
    )


# ---------------------------------------------------------------------------
# PayPal Sandbox Orders
# ---------------------------------------------------------------------------

@app.post("/api/orders/create", response_model=OrderResponse)
async def create_order(request: CreateOrderRequest):
    """
    Create a PayPal Sandbox checkout order for a catalogue product.
    Returns an approval_url to redirect the buyer to PayPal.
    Requires PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET in backend/.env.
    """
    # Validate the product exists in the catalogue
    product = cat.get_product_by_id(request.product_id)
    if product is None:
        raise HTTPException(
            status_code=404,
            detail=f"Product '{request.product_id}' not found in catalogue.",
        )

    # Refuse if credentials are absent (clear error for setup guidance)
    if not pp.credentials_available():
        raise HTTPException(
            status_code=503,
            detail=(
                "PayPal credentials are not configured. "
                "Copy backend/.env.example to backend/.env and add your Sandbox keys, "
                "then restart the backend."
            ),
        )

    try:
        result = await pp.create_order(
            product_id=product.id,
            price=product.price * request.quantity,
            currency=product.currency,
            description=f"{product.title} x {request.quantity}",
            return_url=request.return_url,
            cancel_url=request.cancel_url,
        )
    except pp.PayPalError as exc:
        raise HTTPException(status_code=exc.status_code, detail=str(exc)) from exc

    return OrderResponse(**result)


@app.get("/api/orders/web-return", response_class=HTMLResponse)
def order_web_return(
    token: str = Query(default=""),
    PayerID: str | None = Query(default=None),
    status: str = Query(default="success"),
):
    """
    Landing page for mobile browser PayPal redirects.
    Deep-links back to the Android app via pvca://checkout.
    """
    safe_status = "success" if status == "success" and token else "cancelled"
    deep_link = f"pvca://checkout?token={token}&status={safe_status}"

    html_content = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PVCA - PayPal Sandbox Checkout</title>
<style>
  body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0B132B; color: #fff; text-align: center; padding: 24px; margin: 0; display: flex; align-items: center; justify-content: center; min-height: 100vh; box-sizing: border-box; }}
  .card {{ background: #1C2541; border-radius: 20px; padding: 32px 24px; max-width: 420px; width: 100%; border: 1px solid #3A506B; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }}
  h2 {{ color: #00A699; margin: 0 0 12px; font-size: 22px; }}
  p {{ color: #94A3B8; font-size: 14px; margin: 0 0 24px; line-height: 1.5; }}
  .btn {{ display: inline-block; width: 100%; box-sizing: border-box; padding: 14px 20px; background: #00A699; color: #0B132B; text-decoration: none; border-radius: 12px; font-weight: bold; font-size: 15px; letter-spacing: 0.5px; }}
  .order-id {{ font-family: monospace; font-size: 12px; color: #64748B; margin-top: 16px; word-break: break-all; }}
</style>
</head>
<body>
<div class="card">
  <h2>{'✓ Payment Approved' if safe_status == 'success' else 'Payment Cancelled'}</h2>
  <p>{'Redirecting you back to the PVCA Android App to complete your verification...' if safe_status == 'success' else 'You cancelled this transaction. Return to PVCA.'}</p>
  <a id="btn" class="btn" href="{deep_link}">Return to PVCA App</a>
  {f'<div class="order-id">Order ID: {token}</div>' if token else ''}
</div>
<script>
  setTimeout(function() {{
    window.location.href = "{deep_link}";
  }}, 400);
</script>
</body>
</html>"""
    return HTMLResponse(content=html_content)


@app.post("/api/orders/{order_id}/capture", response_model=CaptureResponse)
async def capture_order(order_id: str):
    """
    Capture an approved PayPal Sandbox order.
    Call this after the buyer approves on PayPal and returns to the site.
    """
    if not pp.credentials_available():
        raise HTTPException(
            status_code=503,
            detail="PayPal credentials are not configured.",
        )

    try:
        result = await pp.capture_order(order_id)
    except pp.PayPalError as exc:
        raise HTTPException(status_code=exc.status_code, detail=str(exc)) from exc

    return CaptureResponse(**result)
