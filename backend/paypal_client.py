"""
PayPal Sandbox API client for PVCA.

Loads credentials from .env (via python-dotenv).
Provides three functions used by the FastAPI routes:
  - get_access_token()
  - create_order(product_id, price, currency, return_url, cancel_url)
  - capture_order(order_id)

All calls go to the PayPal Sandbox base URL only.
"""
from __future__ import annotations

import os
from typing import Optional

import httpx
from dotenv import load_dotenv

load_dotenv()

PAYPAL_CLIENT_ID: str = os.getenv("PAYPAL_CLIENT_ID", "")
PAYPAL_CLIENT_SECRET: str = os.getenv("PAYPAL_CLIENT_SECRET", "")
PAYPAL_BASE_URL: str = os.getenv(
    "PAYPAL_BASE_URL", "https://api-m.sandbox.paypal.com"
)
FRONTEND_RETURN_URL: str = os.getenv("FRONTEND_RETURN_URL", "http://localhost:5173")

_CREDENTIALS_AVAILABLE = bool(PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET)


class PayPalError(Exception):
    """Raised when a PayPal API call fails."""

    def __init__(self, message: str, status_code: int = 500):
        super().__init__(message)
        self.status_code = status_code


def credentials_available() -> bool:
    """Return True when PayPal credentials are present in the environment."""
    return _CREDENTIALS_AVAILABLE


async def get_access_token() -> str:
    """Obtain a short-lived Bearer token from PayPal OAuth2."""
    if not _CREDENTIALS_AVAILABLE:
        raise PayPalError(
            "PayPal credentials not configured. "
            "Copy backend/.env.example to backend/.env and fill in your Sandbox keys.",
            status_code=503,
        )
    async with httpx.AsyncClient() as client:
        resp = await client.post(
            f"{PAYPAL_BASE_URL}/v1/oauth2/token",
            auth=(PAYPAL_CLIENT_ID, PAYPAL_CLIENT_SECRET),
            data={"grant_type": "client_credentials"},
            headers={"Accept": "application/json"},
            timeout=10.0,
        )
    if resp.status_code != 200:
        raise PayPalError(
            f"PayPal token request failed ({resp.status_code}): {resp.text}",
            status_code=502,
        )
    return resp.json()["access_token"]


async def create_order(
    product_id: str,
    price: float,
    currency: str,
    description: str,
    return_url: Optional[str] = None,
    cancel_url: Optional[str] = None,
) -> dict:
    """
    Create a PayPal Checkout order and return
    {"order_id": str, "approval_url": str, "status": str}.
    """
    token = await get_access_token()
    resolved_return = return_url or f"{FRONTEND_RETURN_URL}?paypal_status=success"
    resolved_cancel = cancel_url or f"{FRONTEND_RETURN_URL}?paypal_status=cancelled"

    payload = {
        "intent": "CAPTURE",
        "purchase_units": [
            {
                "reference_id": product_id,
                "description": description[:127],  # PayPal 127-char limit
                "amount": {
                    "currency_code": currency,
                    "value": f"{price:.2f}",
                },
            }
        ],
        "application_context": {
            "return_url": resolved_return,
            "cancel_url": resolved_cancel,
            "brand_name": "PVCA Demo Store",
            "user_action": "PAY_NOW",
            "shipping_preference": "NO_SHIPPING",
        },
    }

    async with httpx.AsyncClient() as client:
        resp = await client.post(
            f"{PAYPAL_BASE_URL}/v2/checkout/orders",
            json=payload,
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
            timeout=15.0,
        )

    if resp.status_code not in (200, 201):
        raise PayPalError(
            f"PayPal create order failed ({resp.status_code}): {resp.text}",
            status_code=502,
        )

    data = resp.json()
    approval_url: Optional[str] = next(
        (link["href"] for link in data.get("links", []) if link["rel"] == "approve"),
        None,
    )
    if not approval_url:
        raise PayPalError("PayPal returned no approval URL in order response.", status_code=502)

    return {
        "order_id": data["id"],
        "approval_url": approval_url,
        "status": data.get("status", "CREATED"),
    }


async def capture_order(order_id: str) -> dict:
    """
    Capture an approved PayPal order.
    Returns {"order_id": str, "status": str, "payer_email": Optional[str]}.
    """
    token = await get_access_token()

    async with httpx.AsyncClient() as client:
        resp = await client.post(
            f"{PAYPAL_BASE_URL}/v2/checkout/orders/{order_id}/capture",
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
            timeout=15.0,
        )

    if resp.status_code not in (200, 201):
        raise PayPalError(
            f"PayPal capture failed ({resp.status_code}): {resp.text}",
            status_code=502,
        )

    data = resp.json()
    payer_email: Optional[str] = (
        data.get("payer", {}).get("email_address")
    )
    return {
        "order_id": data["id"],
        "status": data.get("status", "UNKNOWN"),
        "payer_email": payer_email,
    }
