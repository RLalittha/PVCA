"""
Unit tests for Gemini Vision integration and validation.
No external network calls required (uses mocks and validation tests).
"""
from __future__ import annotations

import base64
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

import pytest
from fastapi.testclient import TestClient
from main import app
import gemini_client as gemini

client = TestClient(app)

# Minimal valid 1x1 JPEG byte stream
TINY_JPEG_BYTES = (
    b"\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00"
    b"\xff\xdb\x00C\x00\x08\x06\x06\x07\x06\x05\x08\x07\x07\x07\t\t"
    b"\x08\n\x0c\x14\r\x0c\x0b\x0b\x0c\x19\x12\x13\x0f\x14\x1d\x1a"
    b"\x1f\x1e\x1d\x1a\x1c\x1c $.' \",#\x1c\x1c(7),01444\x1f'9=82<.342\xff"
    b"\xc0\x00\x0b\x08\x00\x01\x00\x01\x01\x01\x11\x00\xff\xc4\x00\x1f\x00"
    b"\x00\x01\x05\x01\x01\x01\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00"
    b"\x01\x02\x03\x04\x05\x06\x07\x08\t\n\x0b\xff\xda\x00\x08\x01\x01\x00"
    b"\x00?\x00\xbf\x00\xff\xd9"
)
TINY_JPEG_B64 = base64.b64encode(TINY_JPEG_BYTES).decode("ascii")

# Minimal valid 1x1 PNG byte stream
TINY_PNG_BYTES = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01"
    b"\x08\x06\x00\x00\x00\x1f\x15c4\x00\x00\x00\rIDATx\x9cc`\x00\x00\x00"
    b"\x02\x00\x01H\xaf\xa4q\x00\x00\x00\x00IEND\xaeB`\x82"
)
TINY_PNG_B64 = base64.b64encode(TINY_PNG_BYTES).decode("ascii")


def test_validate_image_base64_jpeg():
    raw, mime = gemini.validate_image_base64(TINY_JPEG_B64)
    assert raw.startswith(b"\xff\xd8\xff")
    assert mime == "image/jpeg"


def test_validate_image_base64_data_uri_prefix():
    data_uri = f"data:image/jpeg;base64,{TINY_JPEG_B64}"
    raw, mime = gemini.validate_image_base64(data_uri)
    assert raw.startswith(b"\xff\xd8\xff")
    assert mime == "image/jpeg"


def test_validate_image_base64_png():
    raw, mime = gemini.validate_image_base64(TINY_PNG_B64)
    assert raw.startswith(b"\x89PNG\r\n\x1a\n")
    assert mime == "image/png"


def test_validate_image_base64_empty_raises():
    with pytest.raises(ValueError, match="Image data is empty"):
        gemini.validate_image_base64("")


def test_validate_image_base64_invalid_b64_raises():
    with pytest.raises(ValueError, match="Invalid Base64"):
        gemini.validate_image_base64("not-valid-base-64!!!")


def test_validate_image_base64_unsupported_format():
    fake_payload = base64.b64encode(b"This is just random plain text").decode("ascii")
    with pytest.raises(ValueError, match="Unsupported image format"):
        gemini.validate_image_base64(fake_payload)


def test_validate_image_base64_size_limit():
    with pytest.raises(ValueError, match="exceeds maximum allowed limit"):
        gemini.validate_image_base64(TINY_JPEG_B64, max_size_bytes=10)


def test_identify_endpoint_invalid_image_returns_400():
    r = client.post("/api/products/identify", json={"image_base64": "invalid_b64!!!"})
    assert r.status_code == 400
    assert "Invalid Base64" in r.json()["detail"]


def test_identify_endpoint_unsupported_format_returns_400():
    fake_b64 = base64.b64encode(b"Hello world text file").decode("ascii")
    r = client.post("/api/products/identify", json={"image_base64": fake_b64})
    assert r.status_code == 400
    assert "Unsupported image format" in r.json()["detail"]


def test_identify_endpoint_fallback_when_unconfigured(monkeypatch):
    # Temporarily force API key to be empty
    monkeypatch.setattr(gemini, "GEMINI_API_KEY", "")
    r = client.post("/api/products/identify", json={"image_base64": TINY_JPEG_B64})
    assert r.status_code == 200
    data = r.json()
    assert data["identified"] is False
    assert data["confidence_tier"] == "UNAVAILABLE"
    assert data["source"] == "fallback"


def test_match_user_query_priority_over_gemini(monkeypatch):
    """
    Requirement 2: Manual refinement MUST always take priority over Gemini identification.
    """
    async def mock_identify(*args, **kwargs):
        return "running shoes"

    monkeypatch.setattr(gemini, "identify_cropped_object", mock_identify)
    monkeypatch.setattr(gemini, "is_gemini_configured", lambda: True)

    payload = {
        "detected_category": "General Object",
        "crop_labels": [],
        "scene_labels": [],
        "user_query": "ceiling fan remote",  # User explicitly typed ceiling fan remote
        "crop_image_base64": TINY_JPEG_B64,
    }
    r = client.post("/api/products/match", json=payload)
    assert r.status_code == 200
    data = r.json()
    assert data["matched"] is True
    # The user query must match the ceiling fan remote, NOT the mock Gemini "running shoes"
    assert data["candidate"]["product"]["id"] == "PVCA-ELEC-001"
    assert data["candidate"]["is_user_refined"] is True


def test_match_with_ai_identification(monkeypatch):
    """
    When no user_query is provided, AI identification guides the product match.
    """
    async def mock_identify(*args, **kwargs):
        return "ceiling fan remote control"

    monkeypatch.setattr(gemini, "identify_cropped_object", mock_identify)
    monkeypatch.setattr(gemini, "is_gemini_configured", lambda: True)

    payload = {
        "detected_category": "General Object",
        "crop_labels": [],
        "scene_labels": [],
        "user_query": None,
        "crop_image_base64": TINY_JPEG_B64,
    }
    r = client.post("/api/products/match", json=payload)
    assert r.status_code == 200
    data = r.json()
    assert data["matched"] is True
    assert data["candidate"]["product"]["id"] == "PVCA-ELEC-001"
    assert "AI identified as 'ceiling fan remote control'" in data["candidate"]["evidence_explanation"]
    assert data["identified_as"] == "ceiling fan remote control"


def test_match_fallback_when_gemini_fails(monkeypatch):
    """
    Requirement 4: When Gemini fails/times out, seamlessly fall back to ML Kit matching.
    """
    async def mock_identify_fails(*args, **kwargs):
        return None  # simulates timeout or failure

    monkeypatch.setattr(gemini, "identify_cropped_object", mock_identify_fails)

    payload = {
        "detected_category": "Home good",
        "crop_labels": [{"text": "Cup", "confidence": 0.90}, {"text": "Coffee", "confidence": 0.85}],
        "scene_labels": [],
        "user_query": None,
        "crop_image_base64": TINY_JPEG_B64,
    }
    r = client.post("/api/products/match", json=payload)
    assert r.status_code == 200
    data = r.json()
    assert data["matched"] is True
    # Should fall back to matching the Ceramic Mug via ML Kit crop labels
    assert data["candidate"]["product"]["id"] == "PVCA-BOT-002"
