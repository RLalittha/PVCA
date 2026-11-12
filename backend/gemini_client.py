"""
Gemini Vision client for PVCA.

Loads GEMINI_API_KEY and GEMINI_MODEL from .env (via python-dotenv).
Uses httpx for lightweight, asynchronous API calls to Google's Generative Language API.

Security & Resilience:
- API key is never logged or exposed.
- Image Base64 is strictly validated (size, format, encoding) before dispatch.
- Times out after 6 seconds with automatic graceful fallback (returns None).
"""
from __future__ import annotations

import base64
import logging
import os
import re
from typing import Optional

import httpx
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger("pvca.gemini")

GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "").strip()
# Use active Gemini Flash model by default, configurable via environment
GEMINI_MODEL: str = os.getenv("GEMINI_MODEL", "gemini-2.5-flash").strip()
GEMINI_API_BASE: str = "https://generativelanguage.googleapis.com/v1beta/models"

# Maximum allowable decoded image size (5 MB)
MAX_IMAGE_SIZE_BYTES: int = 5 * 1024 * 1024


def is_gemini_configured() -> bool:
    """Return True when a non-empty Gemini API key is present."""
    return bool(GEMINI_API_KEY)


def validate_image_base64(raw_b64: str, max_size_bytes: int = MAX_IMAGE_SIZE_BYTES) -> tuple[bytes, str]:
    """
    Validate, clean, and decode a Base64 image string.
    Returns (decoded_bytes, detected_mime_type).
    Raises ValueError on invalid format, excessive size, or corrupted payload.
    """
    if not raw_b64 or not raw_b64.strip():
        raise ValueError("Image data is empty.")

    cleaned = raw_b64.strip()
    # Strip optional data URI prefix e.g. "data:image/jpeg;base64,"
    if "," in cleaned and cleaned.startswith("data:"):
        cleaned = cleaned.split(",", 1)[1]

    # Clean whitespace/newlines
    cleaned = re.sub(r"\s+", "", cleaned)

    try:
        image_bytes = base64.b64decode(cleaned, validate=True)
    except Exception as exc:
        raise ValueError("Invalid Base64 encoding.") from exc

    if len(image_bytes) == 0:
        raise ValueError("Decoded image byte stream is empty.")

    if len(image_bytes) > max_size_bytes:
        raise ValueError(
            f"Image size ({len(image_bytes)} bytes) exceeds maximum allowed limit of {max_size_bytes} bytes."
        )

    # Magic-byte MIME type validation
    if image_bytes.startswith(b"\xff\xd8\xff"):
        mime_type = "image/jpeg"
    elif image_bytes.startswith(b"\x89PNG\r\n\x1a\n"):
        mime_type = "image/png"
    elif image_bytes.startswith(b"RIFF") and len(image_bytes) >= 12 and image_bytes[8:12] == b"WEBP":
        mime_type = "image/webp"
    else:
        raise ValueError("Unsupported image format. Allowed formats: JPEG, PNG, WEBP.")

    return image_bytes, mime_type


async def identify_cropped_object(
    image_bytes: bytes,
    mime_type: str = "image/jpeg",
    timeout_seconds: float = 20.0,
) -> Optional[str]:
    """
    Query Gemini Vision to identify the primary object in a cropped image.

    Returns a clean, concise retail description (e.g., 'ceiling fan remote control'),
    or None if Gemini is unavailable, times out, is unconfigured, or cannot identify.
    """
    if not is_gemini_configured():
        return None

    # Determine SSL verification certificate if custom CA is configured
    verify_cert: bool | str = True
    avast_cert = os.getenv("SSL_CERT_FILE", r"D:\PVCA\.tools\avast_root.pem")
    if os.path.exists(avast_cert):
        verify_cert = avast_cert

    url = f"{GEMINI_API_BASE}/{GEMINI_MODEL}:generateContent"
    b64_data = base64.b64encode(image_bytes).decode("ascii")

    prompt_text = (
        "Identify the main physical product in this image. "
        "Return a concise retail description of 3 to 7 words. "
        "The description MUST begin with the product type noun, "
        "such as headphones, shoes, backpack, remote control, watch, "
        "bottle, or keyboard. "
        "After the product type, include useful visible characteristics "
        "such as color, shape, material, or style. "
        "Never return only a brand, adjective, or feature. "
        "Do not guess a brand or model. "
        "Only mention a brand if its logo or name is clearly readable. "
        "If the product type cannot be determined, return exactly UNKNOWN. "
        "Return only the description, without punctuation or explanations."
    )

    payload = {
        "contents": [
            {
                "parts": [
                    {"text": prompt_text},
                    {
                        "inline_data": {
                            "mime_type": mime_type,
                            "data": b64_data,
                        }
                    },
                ]
            }
        ],
        "generationConfig": {
            "temperature": 0.1,
            "maxOutputTokens": 128,
        },
    }

    headers = {
        "Content-Type": "application/json",
        "x-goog-api-key": GEMINI_API_KEY,
    }

    try:
        logger.info("Calling Gemini Vision API with model '%s'...", GEMINI_MODEL)
        async with httpx.AsyncClient(verify=verify_cert, timeout=timeout_seconds) as client:
            resp = await client.post(
                url,
                json=payload,
                headers=headers,
            )

        logger.info("Gemini API HTTP response status: %d", resp.status_code)

        if resp.status_code != 200:
            err_detail = "Unknown error"
            try:
                err_detail = resp.json().get("error", {}).get("message", resp.text[:200])
            except Exception:
                err_detail = resp.text[:200]
            logger.warning("Gemini Vision returned HTTP %d: %s", resp.status_code, err_detail)
            return None

        data = resp.json()
        candidates = data.get("candidates", [])
        if not candidates:
            logger.warning("Gemini Vision returned 200 OK but candidates array is empty.")
            return None

        content = candidates[0].get("content", {})
        parts = content.get("parts", [])
        if not parts:
            logger.warning("Gemini Vision returned candidate with no parts.")
            return None

        raw_text = parts[0].get("text", "")
        # Sanitize text output
        clean_text = raw_text.strip().replace("\n", " ").replace("\r", "")
        clean_text = re.sub(r'["\'`*_#]', "", clean_text).strip()
        # Remove trailing period if present
        clean_text = re.sub(r"\.+$", "", clean_text).strip()

        if not clean_text or len(clean_text) > 80:
            logger.warning("Gemini Vision output was empty or exceeded length: '%s'", clean_text)
            return None

        # Discard generic non-informative labels
        lowered = clean_text.lower()

        generic_labels = {
            "unknown", "object", "item", "product", "image", "none",
            "wireless", "black", "white", "electronic", "portable",
        }

        incomplete_prefixes = (
            "color:", "colour:", "material:", "style:",
            "brand:", "shape:", "feature:",
        )

        if (
            lowered in generic_labels
            or lowered.startswith(incomplete_prefixes)
            or clean_text.endswith((",", ":", ";"))
        ):
            logger.warning(
                "Gemini Vision returned incomplete identification: '%s'",
                clean_text,
            )
            return None

        logger.info("Gemini Vision successfully identified object: '%s'", clean_text)
        return clean_text

    except Exception as exc:
        logger.warning("Gemini Vision request exception: %s: %s", type(exc).__name__, str(exc))
        return None
