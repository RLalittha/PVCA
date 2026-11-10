import React, { useRef, useState, useEffect, useCallback } from 'react';
import { DetectedObject } from '../types';

interface ImageCanvasProps {
  imageSrc: string;
  detectedObjects: DetectedObject[];
  selectedObjectId: number | null;
  onSelectObject: (id: number) => void;
  onImageLoaded?: (img: HTMLImageElement) => void;
}

export const ImageCanvas: React.FC<ImageCanvasProps> = ({
  imageSrc,
  detectedObjects,
  selectedObjectId,
  onSelectObject,
  onImageLoaded,
}) => {
  const imgRef = useRef<HTMLImageElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [scale, setScale] = useState<{ scaleX: number; scaleY: number }>({ scaleX: 1, scaleY: 1 });

  const updateScale = useCallback(() => {
    if (!imgRef.current) return;
    const img = imgRef.current;
    if (img.naturalWidth > 0 && img.clientWidth > 0) {
      setScale({
        scaleX: img.clientWidth / img.naturalWidth,
        scaleY: img.clientHeight / img.naturalHeight,
      });
    }
  }, []);

  useEffect(() => {
    window.addEventListener('resize', updateScale);
    return () => window.removeEventListener('resize', updateScale);
  }, [updateScale]);

  const handleImageLoad = (e: React.SyntheticEvent<HTMLImageElement>) => {
    const img = e.currentTarget;
    updateScale();
    if (onImageLoaded) {
      onImageLoaded(img);
    }
  };

  return (
    <div className="relative flex justify-center items-center overflow-hidden rounded-2xl bg-slate-950 border border-slate-800 shadow-2xl p-2 sm:p-4">
      {/* Container wraps image tightly */}
      <div ref={containerRef} className="relative inline-block max-w-full">
        <img
          ref={imgRef}
          src={imageSrc}
          alt="Uploaded scene"
          onLoad={handleImageLoad}
          className="block max-h-[560px] w-auto max-w-full rounded-xl object-contain shadow-md"
        />

        {/* Overlay SVG and interactive dots */}
        <div className="absolute inset-0 pointer-events-none">
          {detectedObjects.map((obj) => {
            const [x, y, w, h] = obj.bbox;
            const left = x * scale.scaleX;
            const top = y * scale.scaleY;
            const width = w * scale.scaleX;
            const height = h * scale.scaleY;

            const isSelected = obj.id === selectedObjectId;
            const centerX = left + width / 2;
            const centerY = top + height / 2;

            return (
              <React.Fragment key={obj.id}>
                {/* Bounding Box */}
                <div
                  style={{
                    left: `${left}px`,
                    top: `${top}px`,
                    width: `${width}px`,
                    height: `${height}px`,
                  }}
                  className={`absolute rounded-lg transition-all duration-300 pointer-events-none ${
                    isSelected
                      ? 'border-2 border-cyan-400 bg-cyan-500/15 shadow-[0_0_20px_rgba(72,202,228,0.35)]'
                      : 'border border-dashed border-white/50 bg-black/10 hover:border-cyan-300/80'
                  }`}
                >
                  {/* Category Pill Tag above box */}
                  <div
                    className={`absolute -top-6 left-1 text-[11px] font-bold px-2 py-0.5 rounded shadow ${
                      isSelected
                        ? 'bg-cyan-500 text-slate-950 shadow-cyan-500/30'
                        : 'bg-slate-900/90 text-slate-300 border border-slate-700'
                    }`}
                  >
                    #{obj.id} {obj.label} ({(obj.score * 100).toFixed(0)}%)
                  </div>
                </div>

                {/* Glowing, Numbered Cyan Dot at Object Center */}
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    onSelectObject(obj.id);
                  }}
                  style={{
                    left: `${centerX}px`,
                    top: `${centerY}px`,
                    transform: 'translate(-50%, -50%)',
                  }}
                  title={`Select #${obj.id} ${obj.label}`}
                  className={`pointer-events-auto absolute flex items-center justify-center rounded-full transition-transform duration-200 cursor-pointer focus:outline-none ${
                    isSelected
                      ? 'w-9 h-9 bg-cyan-400 text-slate-950 font-black text-sm ring-4 ring-cyan-400/40 shadow-[0_0_25px_#48CAE4] scale-110'
                      : 'w-7 h-7 bg-[#0077B6]/90 text-white font-bold text-xs hover:scale-125 hover:bg-cyan-400 hover:text-slate-950 ring-2 ring-white/60 shadow-[0_0_12px_#0077B6]'
                  }`}
                >
                  #{obj.id}
                </button>
              </React.Fragment>
            );
          })}
        </div>
      </div>
    </div>
  );
};
