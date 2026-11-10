"""
Loads and indexes the demo product catalogue from the shared JSON asset.
"""
from __future__ import annotations

import json
from pathlib import Path
from functools import lru_cache

from models import ProductItem

_CATALOGUE_PATH = (
    Path(__file__).parent.parent
    / "frontend"
    / "android"
    / "app"
    / "src"
    / "main"
    / "assets"
    / "demo_catalogue.json"
)


@lru_cache(maxsize=1)
def load_catalogue() -> list[ProductItem]:
    """Load catalogue once and cache in memory."""
    raw = json.loads(_CATALOGUE_PATH.read_text(encoding="utf-8"))
    return [ProductItem(**item) for item in raw]


def get_product_by_id(product_id: str) -> ProductItem | None:
    return next((p for p in load_catalogue() if p.id == product_id), None)


def search_products(query: str) -> list[ProductItem]:
    q = query.strip().lower()
    if not q:
        return load_catalogue()
    tokens = q.split()
    results: list[ProductItem] = []
    for product in load_catalogue():
        searchable = (
            f"{product.title} {product.brand} {product.category} "
            f"{product.subcategory} {product.description} "
            f"{' '.join(product.keywords)}"
        ).lower()
        if any(t in searchable for t in tokens):
            results.append(product)
    return results
