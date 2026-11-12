"""
Product matching logic â€” Python port of DefaultProductMatchingService.kt.

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


def _product_type(text: str) -> str | None:
    """Identify specific product types without relying on broad categories."""
    import re

    text = text.lower()

    patterns = {
        "cable": r"\b(cables?|usb[\s-]*c|micro[\s-]*usb|charging[\s-]*cords?|data[\s-]*cords?)\b",
        "headphones": r"\b(headphones?|headsets?|earphones?|earbuds?)\b",
        "footwear": r"\b(sneakers?|shoes?|trainers?|footwear|boots?)\b",
        "eyewear": r"\b(glasses|sunglasses|eyeglasses|spectacles|frames)\b",
    }

    for product_type, pattern in patterns.items():
        if re.search(pattern, text):
            return product_type

    return None

def find_best_match(
    detected_category: str,
    crop_labels: list[LabelInput],
    scene_labels: list[LabelInput],
    user_query: str | None,
    ai_identified_label: str | None = None,
) -> MatchResponse:
    all_products = load_catalogue()
    if not all_products:
        return MatchResponse(matched=False, no_match_reason="Product catalogue is empty.")

    # Requirement 2: Manual product refinement ALWAYS takes priority over AI identification
    has_user_query = bool(user_query and user_query.strip())
    clean_user_query = user_query.strip().lower() if has_user_query else ""

    # If no manual user query, use AI identification if available
    has_ai_label = bool(not has_user_query and ai_identified_label and ai_identified_label.strip())
    clean_ai_label = ai_identified_label.strip().lower() if has_ai_label else ""

    active_query = clean_user_query if has_user_query else clean_ai_label
    query_tokens = active_query.split() if active_query else []
    has_any_query = bool(query_tokens)

    scored: list[tuple[ProductItem, float, str]] = []
    for product in all_products:
        # Reject clearly incompatible product types before scoring.
        if active_query:
            requested_type = _product_type(active_query)
            product_text = " ".join(
                str(value or "")
                for value in (
                    product.title,
                    product.description,
                    product.subcategory,
                )
            )
            candidate_type = _product_type(product_text)

            if requested_type and candidate_type != requested_type:
                continue

                # Prevent incompatible eyewear recommendations.
        # Only apply this rule when the query explicitly identifies lens type.
        if active_query and product.category.lower() == "eyewear":
            query_is_optical = any(
                phrase in active_query
                for phrase in (
                    "clear lens",
                    "clear lenses",
                    "optical glasses",
                    "optical frames",
                    "prescription glasses",
                    "eyeglasses",
                    "computer glasses",
                )
            )

            query_is_sunglasses = any(
                phrase in active_query
                for phrase in (
                    "sunglasses",
                    "sun glasses",
                    "polarized lenses",
                    "tinted lenses",
                )
            )

            product_is_sunglasses = (
                product.subcategory.lower() == "sunglasses"
            )

            product_is_optical = (
                product.subcategory.lower() == "optical frames"
            )

            if query_is_optical and product_is_sunglasses:
                continue

            if query_is_sunglasses and product_is_optical:
                continue

        score_cat = _category_score(detected_category, product.category)
        score_crop, matched_kw = _label_overlap_score(crop_labels, product)
        score_scene, _ = _label_overlap_score(scene_labels, product)
        score_query = _user_query_score(query_tokens, product) if has_any_query else 0.0

        if has_user_query:
            # Preserve the existing manual-refinement behavior.
            if not crop_labels and not scene_labels:
                total = 0.80 * score_query + 0.20 * score_cat
            else:
                total = 0.55 * score_query + 0.25 * score_crop + 0.15 * score_cat + 0.05 * score_scene

        elif has_ai_label:
            # Give Gemini's description greater weight when ML Kit
            # provides no supporting crop or scene labels.
            if not crop_labels and not scene_labels:
                total = 0.80 * score_query + 0.20 * score_cat
            else:
                total = 0.55 * score_query + 0.25 * score_crop + 0.15 * score_cat + 0.05 * score_scene
        elif crop_labels:
            total = 0.60 * score_crop + 0.25 * score_cat + 0.15 * score_scene
        else:
            total = 0.60 * score_cat + 0.40 * score_scene

        clamped = max(0.0, min(total, 1.0))
        explanation = _build_explanation(
            has_user_query=has_user_query,
            user_query=user_query if has_user_query else (ai_identified_label if has_ai_label else None),
            score_user=score_query,
            matched_crop_keywords=matched_kw,
            object_category=detected_category,
            product_category=product.category,
        )
        if has_ai_label and score_query > 0.3:
            explanation = f"AI identified as '{ai_identified_label}'. {explanation}"

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
            identified_as=ai_identified_label if has_ai_label else None,
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
            identified_as=ai_identified_label if has_ai_label else None,
        ),
        identified_as=ai_identified_label if has_ai_label else None,
    )
