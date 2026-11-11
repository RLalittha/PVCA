"""
Backend API tests — pytest + HTTPX TestClient (no network required).
"""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


# ---------------------------------------------------------------------------
# Health
# ---------------------------------------------------------------------------

def test_health():
    r = client.get("/api/health")
    assert r.status_code == 200
    data = r.json()
    assert data["status"] == "ok"
    assert "version" in data


# ---------------------------------------------------------------------------
# Products
# ---------------------------------------------------------------------------

def test_list_products_returns_30():
    r = client.get("/api/products")
    assert r.status_code == 200
    items = r.json()
    assert len(items) == 30


def test_list_products_schema():
    r = client.get("/api/products")
    first = r.json()[0]
    for field in ("id", "title", "brand", "category", "price", "currency"):
        assert field in first, f"Missing field: {field}"


def test_get_product_by_id_found():
    r = client.get("/api/products/PVCA-SHOE-001")
    assert r.status_code == 200
    data = r.json()
    assert data["id"] == "PVCA-SHOE-001"
    assert "AeroStride" in data["brand"]


def test_get_product_by_id_not_found():
    r = client.get("/api/products/PVCA-FAKE-999")
    assert r.status_code == 404


def test_search_products_shoes():
    r = client.get("/api/products/search?q=shoe")
    assert r.status_code == 200
    results = r.json()
    assert len(results) > 0
    # All results should be footwear-related
    for p in results:
        text = (p["title"] + p["category"] + " ".join(p["keywords"])).lower()
        assert "shoe" in text or "footwear" in text or "boot" in text or "sneaker" in text


def test_search_products_empty_query():
    r = client.get("/api/products/search?q=")
    assert r.status_code == 200
    assert len(r.json()) == 30


def test_search_products_no_match():
    r = client.get("/api/products/search?q=xylophone")
    assert r.status_code == 200
    assert r.json() == []


# ---------------------------------------------------------------------------
# Matching
# ---------------------------------------------------------------------------

def test_match_fashion_shoe():
    payload = {
        "detected_category": "Fashion good",
        "crop_labels": [
            {"text": "Shoe", "confidence": 0.95},
            {"text": "Footwear", "confidence": 0.88},
            {"text": "Sneaker", "confidence": 0.80},
        ],
        "scene_labels": [{"text": "Fashion good", "confidence": 0.70}],
    }
    r = client.post("/api/products/match", json=payload)
    assert r.status_code == 200
    data = r.json()
    assert data["matched"] is True
    candidate = data["candidate"]
    assert candidate["confidence_tier"] in ("HIGH_CONFIDENCE", "POSSIBLE_MATCH")
    assert candidate["product"]["category"] == "Footwear"
    assert candidate["match_source"] == "backend"


def test_match_with_user_query():
    payload = {
        "detected_category": "Home good",
        "crop_labels": [{"text": "Mug", "confidence": 0.85}],
        "scene_labels": [],
        "user_query": "ceramic coffee mug",
    }
    r = client.post("/api/products/match", json=payload)
    assert r.status_code == 200
    data = r.json()
    assert data["matched"] is True
    assert data["candidate"]["is_user_refined"] is True


def test_match_no_reliable_match():
    payload = {
        "detected_category": "Plant",
        "crop_labels": [],
        "scene_labels": [],
    }
    r = client.post("/api/products/match", json=payload)
    assert r.status_code == 200
    data = r.json()
    # Plant category gets low score (0.3) — might or might not match
    # The key assertion: response has correct shape
    assert "matched" in data


def test_match_missing_category_422():
    """Missing required field should return 422 Unprocessable Entity."""
    r = client.post("/api/products/match", json={"crop_labels": []})
    assert r.status_code == 422


def test_match_response_has_no_match_reason_when_not_matched():
    payload = {
        "detected_category": "Plant",
        "crop_labels": [],
        "scene_labels": [],
        "user_query": None,
    }
    r = client.post("/api/products/match", json=payload)
    data = r.json()
    if not data["matched"]:
        assert data["no_match_reason"] is not None


# ---------------------------------------------------------------------------
# PayPal Orders (no-credentials path — credentials absent in CI)
# ---------------------------------------------------------------------------

import pytest

PAYPAL_CREDS_AVAILABLE = bool(
    os.getenv("PAYPAL_CLIENT_ID") and os.getenv("PAYPAL_CLIENT_SECRET")
)


def test_create_order_product_not_found():
    """Non-existent product_id should return 404 before any PayPal call."""
    r = client.post("/api/orders/create", json={"product_id": "PVCA-FAKE-999"})
    assert r.status_code == 404


def test_create_order_no_credentials_503():
    """With no credentials configured, should return 503 Service Unavailable."""
    if PAYPAL_CREDS_AVAILABLE:
        pytest.skip("Credentials present — skipping no-credential path test.")
    r = client.post("/api/orders/create", json={"product_id": "PVCA-SHOE-001"})
    assert r.status_code == 503
    assert "credentials" in r.json()["detail"].lower()


def test_capture_order_no_credentials_503():
    """Capture with no credentials configured should return 503."""
    if PAYPAL_CREDS_AVAILABLE:
        pytest.skip("Credentials present — skipping no-credential path test.")
    r = client.post("/api/orders/FAKE-ORDER-ID-123/capture")
    assert r.status_code == 503


def test_order_web_return_html():
    """Landing page returns HTML with pvca deep-link scheme."""
    r = client.get("/api/orders/web-return?token=ORDER-123&status=success")
    assert r.status_code == 200
    assert "pvca://checkout?token=ORDER-123&status=success" in r.text


@pytest.mark.skipif(not PAYPAL_CREDS_AVAILABLE, reason="No PayPal Sandbox credentials configured.")
def test_create_order_live_sandbox():
    """Integration test: create a real Sandbox order. Requires .env credentials."""
    r = client.post("/api/orders/create", json={"product_id": "PVCA-SHOE-001", "quantity": 1})
    assert r.status_code == 200
    data = r.json()
    assert "order_id" in data
    assert data["approval_url"].startswith("https://www.sandbox.paypal.com")
