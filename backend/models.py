"""
Pydantic request / response models for the PVCA backend API.
"""
from __future__ import annotations

from typing import Optional
from pydantic import BaseModel, Field


# ---------------------------------------------------------------------------
# Product models
# ---------------------------------------------------------------------------

class ProductItem(BaseModel):
    id: str
    title: str
    brand: str
    category: str
    subcategory: str
    description: str
    keywords: list[str]
    color: str
    price: float
    currency: str
    merchant: str
    productUrl: str
    imageUrl: Optional[str] = None
    verificationStatus: str
    provenance: str
    lastVerified: str


# ---------------------------------------------------------------------------
# Label input (must be defined before MatchRequest references it)
# ---------------------------------------------------------------------------

class LabelInput(BaseModel):
    text: str
    confidence: float = Field(ge=0.0, le=1.0)


# ---------------------------------------------------------------------------
# Match request / response
# ---------------------------------------------------------------------------

class MatchRequest(BaseModel):
    detected_category: str = Field(
        description="ML Kit object category, e.g. 'Fashion good', 'Home good'."
    )
    crop_labels: list[LabelInput] = Field(
        default_factory=list,
        description="Labels from the cropped object image (ML Kit Image Labeling).",
    )
    scene_labels: list[LabelInput] = Field(
        default_factory=list,
        description="Labels from the full scene image.",
    )
    user_query: Optional[str] = Field(
        default=None,
        description="Optional free-text user refinement query.",
    )


class MatchCandidate(BaseModel):
    product: ProductItem
    score: float = Field(ge=0.0, le=1.0)
    confidence_tier: str          # HIGH_CONFIDENCE | POSSIBLE_MATCH | NO_RELIABLE_MATCH
    evidence_explanation: str
    is_user_refined: bool
    match_source: str = "backend"


class MatchResponse(BaseModel):
    matched: bool
    candidate: Optional[MatchCandidate] = None
    no_match_reason: Optional[str] = None
    suggested_category: Optional[str] = None


# ---------------------------------------------------------------------------
# PayPal order models
# ---------------------------------------------------------------------------

class CreateOrderRequest(BaseModel):
    product_id: str = Field(description="Catalogue product ID to purchase.")
    quantity: int = Field(default=1, ge=1, le=10)


class OrderResponse(BaseModel):
    order_id: str
    approval_url: str
    status: str


class CaptureResponse(BaseModel):
    order_id: str
    status: str          # COMPLETED | APPROVED | VOIDED | …
    payer_email: Optional[str] = None
