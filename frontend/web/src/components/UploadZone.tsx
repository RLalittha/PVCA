import React, { useRef } from 'react';
import { Upload, Image as ImageIcon, Sparkles } from 'lucide-react';

interface UploadZoneProps {
  onImageSelected: (dataUrl: string) => void;
  isProcessing: boolean;
}

const SAMPLE_PRESETS = [
  {
    title: 'Running Shoes',
    category: 'Footwear',
    // Local sample image
    url: '/samples/shoes.jpg',
  },
  {
    title: 'Ceramic Coffee Mug',
    category: 'Drinkware',
    // Local sample image
    url: '/samples/mug.jpg',
  },
  {
    title: 'Leather Backpack',
    category: 'Bags & Luggage',
    // Local sample image
    url: '/samples/backpack.jpg',
  },
  {
    title: 'Minimalist Watch',
    category: 'Watches',
    // Local sample image
    url: '/samples/watch.jpg',
  },
];

export const UploadZone: React.FC<UploadZoneProps> = ({ onImageSelected, isProcessing }) => {
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        onImageSelected(reader.result);
      }
    };
    reader.readAsDataURL(file);
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        onImageSelected(reader.result);
      }
    };
    reader.readAsDataURL(file);
  };

  return (
    <div className="space-y-6">
      {/* Drag & Drop Zone */}
      <div
        onDragOver={(e) => e.preventDefault()}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        className="group border-2 border-dashed border-slate-700 hover:border-cyan-400/80 bg-slate-900/60 hover:bg-slate-900/90 rounded-2xl p-8 sm:p-12 text-center cursor-pointer transition-all duration-300 shadow-xl"
      >
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={handleFileChange}
          className="hidden"
        />

        <div className="w-14 h-14 rounded-2xl bg-cyan-950/80 border border-cyan-800/60 text-cyan-400 flex items-center justify-center mx-auto mb-4 group-hover:scale-110 group-hover:bg-cyan-900 transition-all duration-300 shadow-lg shadow-cyan-500/10">
          <Upload className="w-7 h-7" />
        </div>

        <h3 className="text-base sm:text-lg font-bold text-white mb-1">
          Upload an image to detect & shop products
        </h3>
        <p className="text-xs sm:text-sm text-slate-400 max-w-md mx-auto">
          Drag & drop your photo here, or click to browse. Supports JPG, PNG, and WebP.
        </p>
      </div>

      {/* Preset Quick-Try Demos */}
      <div>
        <div className="flex items-center space-x-2 mb-3">
          <Sparkles className="w-4 h-4 text-cyan-400" />
          <span className="text-xs font-bold uppercase tracking-wider text-slate-300">
            Or Quick-Test with Demonstration Presets
          </span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {SAMPLE_PRESETS.map((sample) => (
            <button
              key={sample.title}
              type="button"
              disabled={isProcessing}
              onClick={() => onImageSelected(sample.url)}
              className="flex items-center space-x-2.5 p-2.5 rounded-xl bg-slate-900/80 hover:bg-slate-800 border border-slate-800 hover:border-cyan-500/50 text-left transition disabled:opacity-50 group"
            >
              <div className="w-10 h-10 rounded-lg overflow-hidden bg-slate-950 flex-shrink-0 border border-slate-800">
                <img
                  src={sample.url}
                  alt={sample.title}
                  className="w-full h-full object-cover group-hover:scale-110 transition duration-300"
                />
              </div>
              <div className="overflow-hidden">
                <div className="text-xs font-bold text-slate-200 truncate group-hover:text-cyan-300">
                  {sample.title}
                </div>
                <div className="text-[10px] text-slate-400 flex items-center space-x-1">
                  <ImageIcon className="w-2.5 h-2.5 text-cyan-400" />
                  <span>{sample.category}</span>
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};
