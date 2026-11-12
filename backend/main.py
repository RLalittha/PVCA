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

import logging
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse

logger = logging.getLogger("pvca.backend")

import catalogue as cat
import matching as match_svc
import paypal_client as pp
import gemini_client as gemini
from models import (
    MatchRequest,
    MatchResponse,
    ProductItem,
    CreateOrderRequest,
    OrderResponse,
    CaptureResponse,
    IdentifyRequest,
    IdentifyResponse,
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
        "version": "2.1.0",
        "paypal_configured": pp.credentials_available(),
        "gemini_configured": gemini.is_gemini_configured(),
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
# Matching & Identification
# ---------------------------------------------------------------------------

@app.post("/api/products/identify", response_model=IdentifyResponse)
async def identify_product_crop(request: IdentifyRequest):
    """
    Identify a localized object from its cropped image using Gemini Vision.
    Validates image format and size, and provides graceful fallback.
    """
    # 1. Always validate incoming image format and Base64 first
    try:
        image_bytes, mime_type = gemini.validate_image_base64(request.image_base64)
    except ValueError as err:
        raise HTTPException(status_code=400, detail=str(err))

    # 2. Check if Gemini service is configured
    if not gemini.is_gemini_configured():
        return IdentifyResponse(
            identified=False,
            identified_label=None,
            confidence_tier="UNAVAILABLE",
            source="fallback",
            message="GEMINI_API_KEY is not configured in backend/.env.",
        )

    # 3. Call Gemini Vision
    identified = await gemini.identify_cropped_object(image_bytes, mime_type=mime_type)
    if not identified:
        return IdentifyResponse(
            identified=False,
            identified_label=None,
            confidence_tier="LOW",
            source="gemini",
            message="Gemini could not identify a specific retail product from the crop.",
        )

    return IdentifyResponse(
        identified=True,
        identified_label=identified,
        confidence_tier="HIGH",
        source="gemini",
        message="Product identified via Gemini Vision.",
    )


@app.post("/api/products/match", response_model=MatchResponse)
async def match_product(request: MatchRequest):
    """
    Rank catalogue products against a detected object.
    If no user query is supplied and a crop image is provided, attempts
    automated Gemini identification.
    Manual user_query ALWAYS takes priority over AI identification.
    """
    ai_identified_label: str | None = None

    has_user_query = bool(request.user_query and request.user_query.strip())
    logger.info(
        "Match request: category='%s', crop_labels=%d, user_query='%s', has_crop_image=%s",
        request.detected_category,
        len(request.crop_labels),
        request.user_query or "",
        bool(request.crop_image_base64),
    )

    # Requirement 2: Manual refinement always takes priority
    if not has_user_query and request.crop_image_base64:
        try:
            image_bytes, mime_type = gemini.validate_image_base64(request.crop_image_base64)
            logger.info("Validated crop image: %d bytes, mime: %s", len(image_bytes), mime_type)
            ai_identified_label = await gemini.identify_cropped_object(image_bytes, mime_type=mime_type)
            logger.info("AI identification result: '%s'", ai_identified_label or "None")
        except ValueError as err:
            logger.warning("Crop image validation failed: %s", str(err))
            ai_identified_label = None

    return match_svc.find_best_match(
        detected_category=request.detected_category,
        crop_labels=request.crop_labels,
        scene_labels=request.scene_labels,
        user_query=request.user_query,
        ai_identified_label=ai_identified_label,
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
