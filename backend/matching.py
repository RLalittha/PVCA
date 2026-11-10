"""
Product matching logic — Python port of DefaultProductMatchingService.kt.

Weights:
  With user query : user 50%, crop 30%, category 15%, scene 5%
  Crop labels only: crop 60%, category 25%, scene 15%
  No labels       : category 60%, scene 40%

Thresholds:
  HIGH_CONFIDENCE >= 0.58
  POSSIBLE_MATCH  >= 0.32
"""
from __future__ import annotations

from catalogue import load_catalogue
from models import LabelInput, MatchCandidate, MatchResponse, ProductItem


# ---------------------------------------------------------------------------
# Scoring helpers
# ---------------------------------------------------------------------------

def _category_score(ml_category: str, product_category: str) -> float:
    ml = ml_category.lower()
    cat = product_category.lower()
    if "fashion" in ml:
        return 1.0 if any(k in cat for k in ("footwear", "bag", "watch", "eyewear", "apparel")) else 0.1
    if "home" in ml:
        return 1.0 if any(k in cat for k in ("drinkware", "home", "electronics")) else 0.2
    if "food" in ml:
        return 1.0 if any(k in cat for k in ("drinkware", "food")) else 0.1
    if "place" in ml or "plant" in ml:
        return 0.3
    return 0.5  # General Object


def _label_overlap_score(labels: list[LabelInput], product: ProductItem) -> tuple[float, list[str]]:
    if not labels:
        return 0.0, []
    searchable = f"{product.title} {product.subcategory} {' '.join(product.keywords)}".lower()
    score = 0.0
    matched: list[str] = []
    for label in labels[:6]:
        txt = label.text.lower().strip()
        if len(txt) > 2 and txt in searchable:
            score += label.confidence * 0.40
            matched.append(label.text)
    return min(score, 1.0), list(dict.fromkeys(matched))  # deduplicate, preserve order


def _user_query_score(query_tokens: list[str], product: ProductItem) -> float:
    if not query_tokens:
        return 0.0
    searchable = (
        f"{product.title} {product.brand} {product.category} "
        f"{product.subcategory} {product.description} "
        f"{' '.join(product.keywords)}"
    ).lower()
    matched = sum(1 for t in query_tokens if t in searchable)
    return matched / len(query_tokens)


def _build_explanation(
    has_user_query: bool,
    user_query: str | None,
    score_user: float,
    matched_crop_keywords: list[str],
    object_category: str,
    product_category: str,
) -> str:
    parts: list[str] = []
    if has_user_query and score_user > 0.4:
        parts.append(f'Matched user description "{user_query}".')
    if matched_crop_keywords:
        kw = ", ".join(matched_crop_keywords[:3])
        parts.append(f"Visual crop matched keywords: {kw}.")
    else:
        parts.append(f"Category aligned with detected {object_category} ({product_category}).")
    return " ".join(parts)


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def find_best_match(
    detected_category: str,
    crop_labels: list[LabelInput],
    scene_labels: list[LabelInput],
    user_query: str | None,
) -> MatchResponse:
    all_products = load_catalogue()
    if not all_products:
        return MatchResponse(matched=False, no_match_reason="Product catalogue is empty.")

    has_user_query = bool(user_query and user_query.strip())
    clean_query = user_query.strip().lower() if has_user_query else ""
    query_tokens = clean_query.split() if has_user_query else []

    scored: list[tuple[ProductItem, float, str]] = []
    for product in all_products:
        score_cat = _category_score(detected_category, product.category)
        score_crop, matched_kw = _label_overlap_score(crop_labels, product)
        score_scene, _ = _label_overlap_score(scene_labels, product)
        score_user = _user_query_score(query_tokens, product) if has_user_query else 0.0

        if has_user_query:
            total = 0.50 * score_user + 0.30 * score_crop + 0.15 * score_cat + 0.05 * score_scene
        elif crop_labels:
            total = 0.60 * score_crop + 0.25 * score_cat + 0.15 * score_scene
        else:
            total = 0.60 * score_cat + 0.40 * score_scene

        clamped = max(0.0, min(total, 1.0))
        explanation = _build_explanation(
            has_user_query=has_user_query,
            user_query=user_query,
            score_user=score_user,
            matched_crop_keywords=matched_kw,
            object_category=detected_category,
            product_category=product.category,
        )
        scored.append((product, clamped, explanation))

    if not scored:
        return MatchResponse(matched=False, no_match_reason="No candidates evaluated.")

    best_product, best_score, explanation = max(scored, key=lambda t: t[1])

    if best_score >= 0.58:
        tier = "HIGH_CONFIDENCE"
    elif best_score >= 0.32:
        tier = "POSSIBLE_MATCH"
    else:
        return MatchResponse(
            matched=False,
            no_match_reason=f"No reliable match found for this {detected_category}. You can refine the search below.",
            suggested_category=detected_category,
        )

    return MatchResponse(
        matched=True,
        candidate=MatchCandidate(
            product=best_product,
            score=round(best_score, 4),
            confidence_tier=tier,
            evidence_explanation=explanation,
            is_user_refined=has_user_query,
            match_source="backend",
        ),
    )
