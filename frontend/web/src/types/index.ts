export interface DetectedObject {
  id: number;
  label: string; // COCO-SSD class or label name
  score: number; // 0..1 confidence
  bbox: [number, number, number, number]; // [x, y, width, height] in natural image pixel dimensions
  category: string; // Mapped commercial category (e.g. 'Footwear', 'Drinkware', 'Consumer Electronics')
}

export interface ProductItem {
  id: string;
  title: string;
  brand: string;
  category: string;
  subcategory: string;
  description: string;
  keywords: string[];
  color: string;
  price: number;
  currency: string;
  merchant: string;
  productUrl: string;
  imageUrl: string | null;
  verificationStatus: string;
  provenance: string;
  lastVerified: string;
}

export interface MatchCandidate {
  product: ProductItem;
  score: number;
  confidence_tier: 'HIGH_CONFIDENCE' | 'POSSIBLE_MATCH' | 'NO_RELIABLE_MATCH';
  evidence_explanation: string;
  is_user_refined: boolean;
  match_source: string;
}

export interface MatchResponse {
  matched: boolean;
  candidate: MatchCandidate | null;
  no_match_reason: string | null;
  suggested_category: string | null;
}

// ---------------------------------------------------------------------------
// PayPal checkout types
// ---------------------------------------------------------------------------

export type CheckoutStatus =
  | 'idle'
  | 'creating'
  | 'success'
  | 'cancelled'
  | 'error';

export interface CheckoutState {
  status: CheckoutStatus;
  orderId?: string;
  message?: string;
}
