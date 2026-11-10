import React, { useState } from 'react';
import { MatchResponse, DetectedObject, CheckoutState } from '../types';
import { createPayPalOrder } from '../services/api';
import {
  Tag,
  ShieldCheck,
  Search,
  Sparkles,
  AlertCircle,
  ExternalLink,
  RotateCcw,
  Store,
  Info,
  Loader2,
  CheckCircle2,
  XCircle,
} from 'lucide-react';

interface ProductMatchCardProps {
  selectedObject: DetectedObject | null;
  matchResponse: MatchResponse | null;
  isLoading: boolean;
  onRefineQuery: (query: string) => void;
  onClearRefinement: () => void;
  activeRefinementQuery: string | null;
}

export const ProductMatchCard: React.FC<ProductMatchCardProps> = ({
  selectedObject,
  matchResponse,
  isLoading,
  onRefineQuery,
  onClearRefinement,
  activeRefinementQuery,
}) => {
  const [refinementInput, setRefinementInput] = useState('');
  const [imgError, setImgError] = useState(false);
  const [checkout, setCheckout] = useState<CheckoutState>({ status: 'idle' });

  if (!selectedObject) {
    return (
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-8 text-center">
        <Info className="w-10 h-10 text-cyan-400 mx-auto mb-3 opacity-60" />
        <h3 className="text-lg font-semibold text-slate-200">No Object Selected</h3>
        <p className="text-sm text-slate-400 mt-1 max-w-sm mx-auto">
          Tap one of the glowing numbered dots on the image above to view matching catalogue products and verified pricing.
        </p>
      </div>
    );
  }

  const candidate = matchResponse?.candidate;
  const product = candidate?.product;
  const isDemo = product?.verificationStatus?.toLowerCase().includes('demo');

  const handleRefineSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (refinementInput.trim()) {
      onRefineQuery(refinementInput.trim());
    }
  };

  const handleClear = () => {
    setRefinementInput('');
    onClearRefinement();
  };

  const handlePayPalCheckout = async () => {
    if (!product) return;
    setCheckout({ status: 'creating' });
    try {
      const order = await createPayPalOrder(product.id, 1);
      setCheckout({ status: 'creating', orderId: order.order_id });
      // Redirect to PayPal approval page (same tab).
      // On return, App.tsx reads the URL params and calls capture.
      window.location.href = order.approval_url;
    } catch (err: unknown) {
      setCheckout({
        status: 'error',
        message: err instanceof Error ? err.message : 'Checkout failed. Please try again.',
      });
    }
  };

  return (
    <div className="bg-slate-900/90 border border-slate-800 rounded-2xl shadow-xl overflow-hidden">
      {/* Header Bar */}
      <div className="px-6 py-4 bg-slate-950/60 border-b border-slate-800 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <span className="w-6 h-6 rounded-full bg-cyan-400 text-slate-950 font-black text-xs flex items-center justify-center">
            #{selectedObject.id}
          </span>
          <span className="text-sm font-semibold text-slate-200 capitalize">
            Selected: {selectedObject.label}
          </span>
          <span className="text-xs text-slate-400">({selectedObject.category})</span>
        </div>

        {/* Confidence Tier Badge */}
        {candidate && (
          <div>
            {candidate.confidence_tier === 'HIGH_CONFIDENCE' && (
              <span className="inline-flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-950/80 text-emerald-300 border border-emerald-600/50">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                <span>Demo Match (High Confidence)</span>
              </span>
            )}
            {candidate.confidence_tier === 'POSSIBLE_MATCH' && (
              <span className="inline-flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-950/80 text-amber-300 border border-amber-600/50">
                <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                <span>Possible Match</span>
              </span>
            )}
            {candidate.confidence_tier === 'NO_RELIABLE_MATCH' && (
              <span className="inline-flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-bold bg-rose-950/80 text-rose-300 border border-rose-600/50">
                <AlertCircle className="w-3.5 h-3.5 text-rose-400" />
                <span>No Reliable Match</span>
              </span>
            )}
          </div>
        )}
      </div>

      {/* Loading State */}
      {isLoading ? (
        <div className="p-12 text-center">
          <div className="w-10 h-10 border-4 border-cyan-400/30 border-t-cyan-400 rounded-full animate-spin mx-auto mb-4" />
          <p className="text-sm font-medium text-slate-300">Searching and ranking catalogue products...</p>
        </div>
      ) : matchResponse?.matched && product ? (
        /* Matched Product View */
        <div className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-start">
            {/* Product Thumbnail */}
            <div className="md:col-span-4 rounded-xl overflow-hidden bg-slate-950 border border-slate-800 aspect-square flex items-center justify-center p-3 relative">
              {product.imageUrl && !imgError ? (
                <img
                  src={product.imageUrl}
                  alt={product.title}
                  onError={() => setImgError(true)}
                  className="w-full h-full object-contain rounded-lg"
                />
              ) : (
                <div className="text-center p-4">
                  <Tag className="w-12 h-12 text-cyan-400/40 mx-auto mb-2" />
                  <span className="text-xs font-medium text-slate-400">{product.category}</span>
                </div>
              )}

              {/* Provenance Tag on Image */}
              <div className="absolute bottom-2 left-2 right-2 px-2 py-1 bg-slate-950/90 rounded text-[10px] text-center text-slate-400 border border-slate-800">
                {product.provenance}
              </div>
            </div>

            {/* Product Details */}
            <div className="md:col-span-8 flex flex-col justify-between h-full space-y-4">
              <div>
                <div className="flex items-center justify-between text-xs text-slate-400 mb-1">
                  <span className="font-semibold text-cyan-400 uppercase tracking-wider">{product.brand}</span>
                  <span className="font-mono">{product.id}</span>
                </div>

                <h4 className="text-lg font-bold text-white leading-snug">{product.title}</h4>
                <p className="text-xs text-slate-300 mt-1 line-clamp-2">{product.description}</p>
              </div>

              {/* Price & Merchant */}
              <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800/80 flex items-center justify-between">
                <div>
                  <span className="text-[11px] uppercase tracking-wider text-slate-400 block font-medium">Demonstrated Price</span>
                  <div className="flex items-baseline space-x-1">
                    <span className="text-2xl font-black text-white">
                      ${product.price.toFixed(2)}
                    </span>
                    <span className="text-xs text-slate-400 font-bold">{product.currency}</span>
                  </div>
                </div>

                <div className="text-right">
                  <span className="text-[11px] uppercase tracking-wider text-slate-400 block font-medium flex items-center justify-end space-x-1">
                    <Store className="w-3 h-3 text-cyan-400" />
                    <span>Seller</span>
                  </span>
                  <span className="text-sm font-semibold text-slate-200">{product.merchant}</span>
                </div>
              </div>

              {/* Evidence Explanation */}
              {candidate?.evidence_explanation && (
                <div className="text-xs text-cyan-200/90 bg-cyan-950/40 border border-cyan-800/50 rounded-lg p-3">
                  <span className="font-bold text-cyan-400">Match Evidence: </span>
                  {candidate.evidence_explanation}
                  {candidate.is_user_refined && (
                    <span className="ml-2 font-bold px-1.5 py-0.5 rounded bg-cyan-800/80 text-[10px] uppercase text-cyan-100">
                      User Refined
                    </span>
                  )}
                </div>
              )}

              {/* Checkout Section */}
              <div className="pt-2 space-y-2">
                {/* Checkout feedback messages */}
                {checkout.status === 'error' && (
                  <div className="flex items-start space-x-2 p-3 bg-rose-950/70 border border-rose-800/70 rounded-xl text-xs text-rose-200">
                    <XCircle className="w-4 h-4 text-rose-400 flex-shrink-0 mt-0.5" />
                    <span>{checkout.message}</span>
                  </div>
                )}
                {checkout.status === 'success' && (
                  <div className="flex items-center space-x-2 p-3 bg-emerald-950/70 border border-emerald-800/70 rounded-xl text-xs text-emerald-200">
                    <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                    <span>{checkout.message}</span>
                  </div>
                )}
                {checkout.status === 'cancelled' && (
                  <div className="flex items-center space-x-2 p-3 bg-amber-950/70 border border-amber-800/70 rounded-xl text-xs text-amber-200">
                    <XCircle className="w-4 h-4 text-amber-400 flex-shrink-0" />
                    <span>Payment cancelled. No charge was made.</span>
                  </div>
                )}

                {/* PayPal Checkout Button (Sandbox Demo) */}
                {checkout.status !== 'success' && (
                  <button
                    type="button"
                    onClick={handlePayPalCheckout}
                    disabled={checkout.status === 'creating'}
                    className="w-full py-3 px-4 rounded-xl font-bold text-sm flex items-center justify-center space-x-2 transition disabled:opacity-60 disabled:cursor-not-allowed"
                    style={{ background: '#003087', color: '#fff' }}
                    title="PayPal Sandbox checkout — demonstration only"
                  >
                    {checkout.status === 'creating' ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span>Creating Sandbox Order…</span>
                      </>
                    ) : (
                      <>
                        {/* PayPal logo wordmark (inline SVG) */}
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" className="opacity-90">
                          <path d="M20.067 8.478c.492.315.844.825.983 1.39.516 2.07-.9 4.148-3.26 4.148H16.4l-.67 3.997H13.5l1.98-11.853h4.46c.95 0 1.74.44 2.127 1.139l-2 2.179z"/>
                          <path d="M9.804 7.158H14c3.185 0 5.093 2.56 4.44 5.728C17.754 15.5 15.4 17 13 17h-1.95l-.6 3.5H8.2l1.604-13.342z"/>
                        </svg>
                        <span>Checkout with PayPal Sandbox</span>
                      </>
                    )}
                  </button>
                )}

                {/* Store Link — still disabled for demo catalogue */}
                <button
                  type="button"
                  disabled
                  className="w-full py-2 px-4 rounded-xl bg-slate-800/60 text-slate-400 font-semibold text-xs border border-slate-700/60 flex items-center justify-center space-x-2 cursor-not-allowed opacity-70"
                  title="Fictional demonstration link is safely disabled"
                >
                  <ExternalLink className="w-4 h-4 opacity-50" />
                  <span>Store Link Disabled (Demo Catalogue Product)</span>
                </button>

                {isDemo && checkout.status === 'idle' && (
                  <p className="text-[11px] text-slate-500 text-center">
                    PayPal Sandbox demo — no real money is charged. Configure credentials in <code className="text-slate-400">backend/.env</code>.
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* No Reliable Match View */
        <div className="p-8 text-center">
          <AlertCircle className="w-12 h-12 text-amber-400/80 mx-auto mb-3" />
          <h4 className="text-base font-bold text-slate-200">No Reliable Match in Catalogue</h4>
          <p className="text-xs text-slate-400 mt-1 max-w-md mx-auto">
            {matchResponse?.no_match_reason ||
              `We couldn't confidently identify this ${selectedObject.label} in our current 30-item demo catalogue. Try refining below.`}
          </p>
        </div>
      )}

      {/* User Search Refinement Input */}
      <div className="p-4 bg-slate-950/90 border-t border-slate-800">
        <form onSubmit={handleRefineSubmit} className="flex flex-col sm:flex-row gap-2">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={refinementInput}
              onChange={(e) => setRefinementInput(e.target.value)}
              placeholder={
                activeRefinementQuery
                  ? `Current: "${activeRefinementQuery}" — enter new keywords to re-rank...`
                  : `Not what you're looking for? Refine search (e.g. "running shoe", "ceramic mug")...`
              }
              className="w-full pl-9 pr-4 py-2 text-xs rounded-xl bg-slate-900 border border-slate-700 text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400 transition"
            />
          </div>

          <div className="flex space-x-2">
            <button
              type="submit"
              disabled={!refinementInput.trim()}
              className="px-4 py-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 disabled:opacity-40 disabled:hover:bg-cyan-500 text-slate-950 font-bold text-xs transition flex items-center justify-center space-x-1"
            >
              <Search className="w-3.5 h-3.5" />
              <span>Refine</span>
            </button>

            {activeRefinementQuery && (
              <button
                type="button"
                onClick={handleClear}
                className="px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium text-xs transition flex items-center space-x-1"
                title="Clear refinement"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Reset</span>
              </button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
};
