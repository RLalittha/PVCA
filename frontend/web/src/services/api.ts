import { MatchResponse } from '../types';

export const API_BASE = '/api';

export async function checkBackendHealth(): Promise<{ online: boolean; version?: string; error?: string }> {
  try {
    const res = await fetch(`${API_BASE}/health`, { method: 'GET' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    return { online: true, version: data.version };
  } catch (err: unknown) {
    return { online: false, error: err instanceof Error ? err.message : 'Unreachable' };
  }
}

export async function matchProductWithBackend(
  detectedCategory: string,
  cropLabels: Array<{ text: string; confidence: number }>,
  sceneLabels: Array<{ text: string; confidence: number }>,
  userQuery?: string
): Promise<MatchResponse> {
  const response = await fetch(`${API_BASE}/products/match`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
    body: JSON.stringify({
      detected_category: detectedCategory,
      crop_labels: cropLabels,
      scene_labels: sceneLabels,
      user_query: userQuery?.trim() || null,
    }),
  });

  if (!response.ok) {
    throw new Error(`Backend error (${response.status}): ${await response.text()}`);
  }

  return response.json();
}

// ---------------------------------------------------------------------------
// PayPal Sandbox order functions
// ---------------------------------------------------------------------------

export async function createPayPalOrder(
  productId: string,
  quantity = 1
): Promise<{ order_id: string; approval_url: string; status: string }> {
  const response = await fetch(`${API_BASE}/orders/create`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
    body: JSON.stringify({ product_id: productId, quantity }),
  });

  if (!response.ok) {
    const detail = await response.json().catch(() => ({ detail: response.statusText }));
    throw new Error(detail.detail ?? `Order creation failed (${response.status})`);
  }

  return response.json();
}

export async function capturePayPalOrder(
  orderId: string
): Promise<{ order_id: string; status: string; payer_email?: string }> {
  const response = await fetch(`${API_BASE}/orders/${orderId}/capture`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    const detail = await response.json().catch(() => ({ detail: response.statusText }));
    throw new Error(detail.detail ?? `Order capture failed (${response.status})`);
  }

  return response.json();
}
