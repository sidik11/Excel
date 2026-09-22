import React from 'react';
import { DisplayImageItem } from '../types';
import { Image as ImageIcon, Sparkles, Maximize2 } from 'lucide-react';

interface ImageGalleryProps {
  images: DisplayImageItem[];
  hasExcel: boolean;
  hasFolder: boolean;
  gridColumns: number;
  imageFitMode: 'fit' | 'crop';
  highContrastBorders: boolean;
  onImageClick: (index: number) => void;
  onLoadSampleClick: () => void;
}

export const ImageGallery: React.FC<ImageGalleryProps> = ({
  images,
  hasExcel,
  hasFolder,
  gridColumns,
  imageFitMode,
  highContrastBorders,
  onImageClick,
  onLoadSampleClick,
}) => {
  if (images.length === 0) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-8 text-center min-h-[420px]">
        <div className="w-16 h-16 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 mb-4 shadow-inner">
          <ImageIcon className="w-8 h-8 opacity-70" />
        </div>
        <h3 className="text-lg font-bold text-slate-200 mb-1">No Catalog Images Displayed</h3>
        <p className="text-sm text-slate-400 max-w-md mb-6">
          {!hasExcel
            ? 'Import an Excel spreadsheet (.xlsx) with CODE, NAME, and TOP COLOR columns, or instantly load our sample catalog.'
            : 'No image matches the current filters or folder. Try selecting a different name/color or import image files.'}
        </p>

        <button
          onClick={onLoadSampleClick}
          id="btn_gallery_load_sample"
          className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-semibold text-sm shadow-lg shadow-indigo-600/25 transition active:scale-95"
        >
          <Sparkles className="w-4 h-4" />
          <span>Load Sample Dress Catalog</span>
        </button>
      </div>
    );
  }

  // Dynamic grid classes based on gridColumns (2, 3, 4, 5)
  const gridClasses = {
    2: 'grid-cols-1 sm:grid-cols-2',
    3: 'grid-cols-2 sm:grid-cols-3 lg:grid-cols-3 xl:grid-cols-4',
    4: 'grid-cols-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5',
    5: 'grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6'
  }[gridColumns] || 'grid-cols-2 sm:grid-cols-3 lg:grid-cols-4';

  return (
    <div className="flex-1 p-4 overflow-y-auto">
      <div className="max-w-7xl mx-auto">
        <div className={`grid ${gridClasses} gap-4`}>
          {images.map((item, index) => (
            <div
              key={item.id + '_' + index}
              onClick={() => onImageClick(index)}
              className={`group relative bg-slate-900/90 rounded-2xl overflow-hidden cursor-pointer transition-all duration-200 hover:-translate-y-1 hover:shadow-xl hover:shadow-indigo-500/10 ${
                highContrastBorders
                  ? 'border-2 border-slate-700/80 hover:border-indigo-500'
                  : 'border border-slate-800 hover:border-slate-700'
              }`}
            >
              {/* Image Preview Container */}
              <div className="relative aspect-square w-full bg-slate-950 flex items-center justify-center overflow-hidden">
                <img
                  src={item.fileUri}
                  alt={item.name}
                  loading="lazy"
                  className={`w-full h-full transition-transform duration-300 group-hover:scale-105 ${
                    imageFitMode === 'crop' ? 'object-cover' : 'object-contain p-2'
                  }`}
                />

                {/* Hover overlay with zoom hint */}
                <div className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center backdrop-blur-[2px]">
                  <span className="p-2 rounded-xl bg-slate-900/80 text-white shadow-lg border border-slate-700">
                    <Maximize2 className="w-5 h-5 text-indigo-400" />
                  </span>
                </div>

                {/* Top Code Badge */}
                <div className="absolute top-2.5 left-2.5">
                  <span className="px-2 py-0.5 rounded-lg text-[11px] font-mono font-bold bg-slate-950/80 text-indigo-300 border border-slate-700/80 shadow">
                    {item.code}
                  </span>
                </div>

                {/* Row Number Badge (if available) */}
                {item.rowNumber && (
                  <div className="absolute top-2.5 right-2.5">
                    <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-slate-900/70 text-slate-400 border border-slate-800">
                      #{item.rowNumber}
                    </span>
                  </div>
                )}
              </div>

              {/* Card Meta Footer */}
              <div className="p-3 bg-slate-900/80 border-t border-slate-800/80">
                <h4 className="text-sm font-bold text-slate-100 truncate mb-1" title={item.name}>
                  {item.name}
                </h4>
                <div className="flex items-center justify-between text-xs">
                  <span className="px-2 py-0.5 rounded-md text-[11px] font-medium bg-slate-800 text-slate-300 border border-slate-700/60 truncate max-w-[140px]">
                    {item.colour}
                  </span>
                  <span className="text-[11px] text-slate-500 font-mono">
                    #{index + 1}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
