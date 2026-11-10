import { useState, useEffect, useCallback } from 'react';
import { Header } from './components/Header';
import { UploadZone } from './components/UploadZone';
import { ImageCanvas } from './components/ImageCanvas';
import { ProductMatchCard } from './components/ProductMatchCard';
import { checkBackendHealth, matchProductWithBackend, capturePayPalOrder } from './services/api';
import { detectObjects } from './services/detector';
import { DetectedObject, MatchResponse, CheckoutState } from './types';
import { RotateCcw, Crosshair, AlertCircle, CheckCircle2, XCircle } from 'lucide-react';

export function App() {
  const [backendOnline, setBackendOnline] = useState<boolean | null>(null);
  const [backendVersion, setBackendVersion] = useState<string | undefined>(undefined);

  const [imageSrc, setImageSrc] = useState<string | null>(null);
  const [detectedObjects, setDetectedObjects] = useState<DetectedObject[]>([]);
  const [selectedObjectId, setSelectedObjectId] = useState<number | null>(null);

  const [matchResponse, setMatchResponse] = useState<MatchResponse | null>(null);
  const [isDetecting, setIsDetecting] = useState<boolean>(false);
  const [isMatching, setIsMatching] = useState<boolean>(false);
  const [detectionError, setDetectionError] = useState<string | null>(null);

  const [activeRefinementQuery, setActiveRefinementQuery] = useState<string | null>(null);

  // PayPal return banner (shown at top of page after redirect back from PayPal)
  const [paypalBanner, setPaypalBanner] = useState<CheckoutState | null>(null);

  // Check backend health on initial mount
  useEffect(() => {
    checkBackendHealth().then((status) => {
      setBackendOnline(status.online);
      setBackendVersion(status.version);
    });
  }, []);

  // On mount: check if we just returned from PayPal approval
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paypalStatus = params.get('paypal_status');
    const token = params.get('token'); // PayPal order ID returned as `token`

    if (paypalStatus === 'success' && token) {
      setPaypalBanner({ status: 'creating', message: 'Capturing PayPal payment…' });
      capturePayPalOrder(token)
        .then((result) => {
          if (result.status === 'COMPLETED') {
            setPaypalBanner({
              status: 'success',
              orderId: result.order_id,
              message: `Payment completed!${result.payer_email ? ` Paid by ${result.payer_email}.` : ''} Order: ${result.order_id}`,
            });
          } else {
            setPaypalBanner({
              status: 'error',
              message: `Order ${result.order_id} has status: ${result.status}. Please contact support.`,
            });
          }
        })
        .catch((err: unknown) => {
          setPaypalBanner({
            status: 'error',
            message: err instanceof Error ? err.message : 'Payment capture failed.',
          });
        });
      // Clean URL without reloading the page
      window.history.replaceState({}, '', window.location.pathname);
    } else if (paypalStatus === 'cancelled') {
      setPaypalBanner({ status: 'cancelled' });
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, []);

  // When an object is selected or query refined, trigger matching
  const runMatching = useCallback(
    async (obj: DetectedObject, userQuery?: string) => {
      setIsMatching(true);
      try {
        // Build crop labels from detection metadata
        const cropLabels = [
          { text: obj.label, confidence: obj.score },
          { text: obj.category, confidence: 0.8 },
        ];
        const sceneLabels = detectedObjects.map((o) => ({
          text: o.label,
          confidence: o.score,
        }));

        const result = await matchProductWithBackend(
          obj.category,
          cropLabels,
          sceneLabels,
          userQuery
        );
        setMatchResponse(result);
      } catch (err: unknown) {
        console.error('Matching failed:', err);
        setMatchResponse({
          matched: false,
          candidate: null,
          no_match_reason: err instanceof Error ? err.message : 'Matching service unavailable',
          suggested_category: obj.category,
        });
      } finally {
        setIsMatching(false);
      }
    },
    [detectedObjects]
  );

  // Called when image is selected from upload or preset
  const handleImageSelected = (src: string) => {
    setImageSrc(src);
    setDetectedObjects([]);
    setSelectedObjectId(null);
    setMatchResponse(null);
    setDetectionError(null);
    setActiveRefinementQuery(null);
    setIsDetecting(true);
  };

  // Called when the <img> in ImageCanvas has finished loading
  const handleImageLoaded = async (img: HTMLImageElement) => {
    try {
      setIsDetecting(true);
      setDetectionError(null);
      const objects = await detectObjects(img);
      setDetectedObjects(objects);

      if (objects.length > 0) {
        const first = objects[0];
        setSelectedObjectId(first.id);
        runMatching(first);
      } else {
        setSelectedObjectId(null);
        setMatchResponse(null);
      }
    } catch (err: unknown) {
      console.error('Object detection failed:', err);
      setDetectionError(
        err instanceof Error ? err.message : 'Failed to run browser object detection'
      );
    } finally {
      setIsDetecting(false);
    }
  };

  const handleSelectObject = (id: number) => {
    if (id === selectedObjectId) return;
    setSelectedObjectId(id);
    setActiveRefinementQuery(null);

    const obj = detectedObjects.find((o) => o.id === id);
    if (obj) {
      runMatching(obj);
    }
  };

  const handleRefineQuery = (query: string) => {
    setActiveRefinementQuery(query);
    const obj = detectedObjects.find((o) => o.id === selectedObjectId);
    if (obj) {
      runMatching(obj, query);
    }
  };

  const handleClearRefinement = () => {
    setActiveRefinementQuery(null);
    const obj = detectedObjects.find((o) => o.id === selectedObjectId);
    if (obj) {
      runMatching(obj);
    }
  };

  const handleResetImage = () => {
    setImageSrc(null);
    setDetectedObjects([]);
    setSelectedObjectId(null);
    setMatchResponse(null);
    setActiveRefinementQuery(null);
  };

  const selectedObject = detectedObjects.find((o) => o.id === selectedObjectId) || null;

  return (
    <div className="min-h-screen bg-[#0B132B] text-slate-100 flex flex-col font-sans">
      <Header backendOnline={backendOnline} backendVersion={backendVersion} />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* PayPal Return Banner */}
        {paypalBanner && (
          <div className={`mb-6 p-4 rounded-xl border flex items-start space-x-3 text-sm ${
            paypalBanner.status === 'success'
              ? 'bg-emerald-950/80 border-emerald-700/80 text-emerald-200'
              : paypalBanner.status === 'cancelled'
              ? 'bg-amber-950/80 border-amber-700/80 text-amber-200'
              : paypalBanner.status === 'error'
              ? 'bg-rose-950/80 border-rose-700/80 text-rose-200'
              : 'bg-slate-900 border-slate-700 text-slate-200'
          }`}>
            {paypalBanner.status === 'success' && <CheckCircle2 className="w-5 h-5 text-emerald-400 flex-shrink-0 mt-0.5" />}
            {paypalBanner.status === 'cancelled' && <XCircle className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />}
            {paypalBanner.status === 'error' && <AlertCircle className="w-5 h-5 text-rose-400 flex-shrink-0 mt-0.5" />}
            <div>
              {paypalBanner.status === 'success' && <p className="font-bold">PayPal Sandbox Payment Completed ✓</p>}
              {paypalBanner.status === 'cancelled' && <p className="font-bold">Payment Cancelled</p>}
              {paypalBanner.status === 'error' && <p className="font-bold">Payment Error</p>}
              {paypalBanner.status === 'creating' && <p className="font-bold">Processing…</p>}
              {paypalBanner.message && <p className="text-xs mt-0.5 opacity-90">{paypalBanner.message}</p>}
              {paypalBanner.status === 'cancelled' && (
                <p className="text-xs mt-0.5 opacity-90">No charge was made. You can try again.</p>
              )}
            </div>
            <button
              onClick={() => setPaypalBanner(null)}
              className="ml-auto text-current opacity-50 hover:opacity-100 text-lg leading-none flex-shrink-0"
              title="Dismiss"
            >×</button>
          </div>
        )}
        {!imageSrc ? (
          /* Empty / Upload View */
          <div className="max-w-3xl mx-auto py-8">
            <div className="text-center mb-8">
              <span className="text-xs uppercase font-extrabold tracking-widest text-cyan-400 bg-cyan-950/80 px-3 py-1 rounded-full border border-cyan-800/60 inline-block mb-3">
                PayPal Visual Commerce Assistant
              </span>
              <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
                Visual Discovery to Seamless Commerce
              </h1>
              <p className="mt-2 text-sm sm:text-base text-slate-300 max-w-xl mx-auto">
                Upload any picture to detect items with AI, tap interactive numbered dots, and retrieve verified catalogue pricing.
              </p>
            </div>

            <UploadZone onImageSelected={handleImageSelected} isProcessing={isDetecting} />
          </div>
        ) : (
          /* Active Analysis View */
          <div className="space-y-6">
            {/* Top Toolbar */}
            <div className="flex items-center justify-between bg-slate-900/80 border border-slate-800 rounded-xl px-4 py-2.5">
              <div className="flex items-center space-x-2 text-xs text-slate-300">
                <Crosshair className="w-4 h-4 text-cyan-400" />
                <span>
                  Detected <strong className="text-white">{detectedObjects.length}</strong> objects in scene
                </span>
                {isDetecting && (
                  <span className="text-cyan-400 flex items-center space-x-1 ml-2">
                    <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping inline-block" />
                    <span>Running COCO-SSD Detector...</span>
                  </span>
                )}
              </div>

              <button
                type="button"
                onClick={handleResetImage}
                className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 transition"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Choose Another Photo</span>
              </button>
            </div>

            {/* Error Banner */}
            {detectionError && (
              <div className="p-4 rounded-xl bg-rose-950/80 border border-rose-800/80 text-rose-200 text-xs flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 text-rose-400 flex-shrink-0" />
                <span>{detectionError}</span>
              </div>
            )}

            {/* Main Interactive Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
              {/* Left Column: Image Canvas & Detection Tabs */}
              <div className="lg:col-span-7 space-y-4">
                <ImageCanvas
                  imageSrc={imageSrc}
                  detectedObjects={detectedObjects}
                  selectedObjectId={selectedObjectId}
                  onSelectObject={handleSelectObject}
                  onImageLoaded={handleImageLoaded}
                />

                {/* Detected Objects Selector Chips */}
                {detectedObjects.length > 0 && (
                  <div className="p-3 bg-slate-900/80 border border-slate-800 rounded-xl">
                    <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400 block mb-2">
                      Detected Objects (Tap dot or pill to switch):
                    </span>
                    <div className="flex flex-wrap gap-2">
                      {detectedObjects.map((obj) => (
                        <button
                          key={obj.id}
                          type="button"
                          onClick={() => handleSelectObject(obj.id)}
                          className={`flex items-center space-x-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                            obj.id === selectedObjectId
                              ? 'bg-cyan-500 text-slate-950 shadow-md shadow-cyan-500/20 font-bold'
                              : 'bg-slate-800/90 hover:bg-slate-700 text-slate-300 border border-slate-700/60'
                          }`}
                        >
                          <span
                            className={`w-4 h-4 rounded-full text-[10px] flex items-center justify-center font-black ${
                              obj.id === selectedObjectId
                                ? 'bg-slate-950 text-cyan-400'
                                : 'bg-cyan-500 text-slate-950'
                            }`}
                          >
                            {obj.id}
                          </span>
                          <span className="capitalize">{obj.label}</span>
                          <span className="text-[10px] opacity-70">
                            {(obj.score * 100).toFixed(0)}%
                          </span>
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Right Column: Best Product Match Card & Refinement */}
              <div className="lg:col-span-5">
                <ProductMatchCard
                  selectedObject={selectedObject}
                  matchResponse={matchResponse}
                  isLoading={isMatching}
                  onRefineQuery={handleRefineQuery}
                  onClearRefinement={handleClearRefinement}
                  activeRefinementQuery={activeRefinementQuery}
                />
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 py-4 mt-auto text-center text-xs text-slate-500">
        <p>PVCA — PayPal Visual Commerce Assistant • Primary Web Demonstration & Android Mobile Prototype</p>
      </footer>
    </div>
  );
}
