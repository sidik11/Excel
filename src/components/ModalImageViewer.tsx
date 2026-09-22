import React, { useState, useEffect, useCallback } from 'react';
import { DisplayImageItem } from '../types';
import { X, ChevronLeft, ChevronRight, Play, Pause, ZoomIn, ZoomOut, RotateCcw, Info, FastForward } from 'lucide-react';

interface ModalImageViewerProps {
  images: DisplayImageItem[];
  currentIndex: number;
  isSlideshowActive: boolean;
  slideshowSpeedMs: number;
  onClose: () => void;
  onNext: () => void;
  onPrevious: () => void;
  onToggleSlideshow: () => void;
  onSpeedChange?: (speedMs: number) => void;
}

export const ModalImageViewer: React.FC<ModalImageViewerProps> = ({
  images,
  currentIndex,
  isSlideshowActive,
  slideshowSpeedMs,
  onClose,
  onNext,
  onPrevious,
  onToggleSlideshow,
  onSpeedChange,
}) => {
  const [scale, setScale] = useState(1);
  const [showInfo, setShowInfo] = useState(true);
  const [isFastForward, setIsFastForward] = useState(false);

  const currentImage = images[currentIndex];

  const handleZoomIn = () => setScale(prev => Math.min(prev + 0.3, 3));
  const handleZoomOut = () => setScale(prev => Math.max(prev - 0.3, 0.5));
  const handleResetZoom = () => setScale(1);

  // Keyboard navigation
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight') onNext();
      if (e.key === 'ArrowLeft') onPrevious();
      if (e.key === 'Escape') onClose();
      if (e.key === ' ') {
        e.preventDefault();
        onToggleSlideshow();
      }
    },
    [onNext, onPrevious, onClose, onToggleSlideshow]
  );

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  // Slideshow timer
  useEffect(() => {
    if (!isSlideshowActive) return;
    const intervalTime = isFastForward ? 350 : slideshowSpeedMs || 1500;
    const timer = setInterval(() => {
      onNext();
    }, intervalTime);

    return () => clearInterval(timer);
  }, [isSlideshowActive, isFastForward, slideshowSpeedMs, onNext]);

  // Reset zoom on slide change
  useEffect(() => {
    setScale(1);
  }, [currentIndex]);

  if (!currentImage) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/95 flex flex-col justify-between backdrop-blur-md select-none">
      {/* Top Header Bar */}
      <div className="flex items-center justify-between px-4 py-3 bg-slate-900/80 border-b border-slate-800 z-10">
        <div className="flex items-center gap-3">
          <span className="font-mono text-xs px-2.5 py-1 rounded-md bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
            {currentIndex + 1} / {images.length}
          </span>
          <span className="text-sm font-semibold text-slate-200 truncate max-w-[200px] sm:max-w-md">
            {currentImage.name} - {currentImage.code}
          </span>
        </div>

        <div className="flex items-center gap-2">
          {/* Zoom controls */}
          <div className="hidden sm:flex items-center gap-1 bg-slate-800 rounded-lg p-1 border border-slate-700">
            <button
              onClick={handleZoomOut}
              className="p-1.5 text-slate-300 hover:text-white rounded hover:bg-slate-700"
              title="Zoom Out"
            >
              <ZoomOut className="w-4 h-4" />
            </button>
            <span className="text-xs font-mono text-slate-400 px-1">{Math.round(scale * 100)}%</span>
            <button
              onClick={handleZoomIn}
              className="p-1.5 text-slate-300 hover:text-white rounded hover:bg-slate-700"
              title="Zoom In"
            >
              <ZoomIn className="w-4 h-4" />
            </button>
            {scale !== 1 && (
              <button
                onClick={handleResetZoom}
                className="p-1.5 text-slate-300 hover:text-white rounded hover:bg-slate-700"
                title="Reset Zoom"
              >
                <RotateCcw className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          <button
            onClick={() => setShowInfo(!showInfo)}
            className={`p-2 rounded-lg border transition ${
              showInfo ? 'bg-indigo-600/30 border-indigo-500/50 text-indigo-300' : 'bg-slate-800 border-slate-700 text-slate-400'
            }`}
            title="Toggle Details Info"
          >
            <Info className="w-4 h-4" />
          </button>

          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-slate-800 hover:bg-red-500/20 hover:text-red-400 text-slate-300 border border-slate-700 transition"
            title="Close Lightbox (Esc)"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Main Image Stage */}
      <div className="relative flex-1 flex items-center justify-center p-4 overflow-hidden">
        {/* Navigation arrows */}
        <button
          onClick={onPrevious}
          id="btn_modal_prev_image"
          className="absolute left-4 top-1/2 -translate-y-1/2 p-3 rounded-2xl bg-slate-900/80 hover:bg-slate-800 text-white border border-slate-700/80 shadow-2xl z-20 transition active:scale-95"
          title="Previous Image (Left Arrow)"
        >
          <ChevronLeft className="w-6 h-6" />
        </button>

        <button
          onClick={onNext}
          id="btn_modal_next_image"
          className="absolute right-4 top-1/2 -translate-y-1/2 p-3 rounded-2xl bg-slate-900/80 hover:bg-slate-800 text-white border border-slate-700/80 shadow-2xl z-20 transition active:scale-95"
          title="Next Image (Right Arrow)"
        >
          <ChevronRight className="w-6 h-6" />
        </button>

        {/* Displayed Image */}
        <div className="max-w-full max-h-full flex items-center justify-center transition-transform duration-200">
          <img
            src={currentImage.fileUri}
            alt={currentImage.name}
            style={{ transform: `scale(${scale})` }}
            className="max-w-[90vw] max-h-[75vh] object-contain rounded-xl shadow-2xl transition-transform"
          />
        </div>

        {/* Info overlay card */}
        {showInfo && (
          <div className="absolute bottom-6 left-6 max-w-sm bg-slate-900/90 border border-slate-700/80 p-3.5 rounded-2xl shadow-2xl backdrop-blur-md text-xs z-20">
            <div className="flex items-center justify-between gap-2 mb-1.5">
              <span className="font-bold text-slate-100 text-sm">{currentImage.name}</span>
              <span className="font-mono font-bold text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded border border-indigo-500/20">
                {currentImage.code}
              </span>
            </div>
            <div className="flex items-center gap-2 text-slate-300">
              <span className="text-slate-400">Color:</span>
              <span className="font-semibold text-emerald-400">{currentImage.colour}</span>
            </div>
            {currentImage.rowNumber && (
              <div className="text-[11px] text-slate-400 mt-1">Excel Row #{currentImage.rowNumber}</div>
            )}
          </div>
        )}
      </div>

      {/* Bottom Slideshow Controls */}
      <div className="flex items-center justify-center gap-3 px-4 py-3 bg-slate-900/80 border-t border-slate-800 z-10">
        <button
          onClick={onToggleSlideshow}
          id="btn_toggle_slideshow"
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold shadow-lg transition active:scale-95 ${
            isSlideshowActive
              ? 'bg-amber-600 text-white shadow-amber-600/20'
              : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-indigo-600/20'
          }`}
        >
          {isSlideshowActive ? (
            <>
              <Pause className="w-4 h-4" />
              <span>Pause Slideshow</span>
            </>
          ) : (
            <>
              <Play className="w-4 h-4" />
              <span>Play Slideshow</span>
            </>
          )}
        </button>

        {isSlideshowActive && (
          <button
            onClick={() => setIsFastForward(!isFastForward)}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold border transition ${
              isFastForward
                ? 'bg-rose-500/20 border-rose-500/40 text-rose-300'
                : 'bg-slate-800 border-slate-700 text-slate-300 hover:bg-slate-700'
            }`}
            title="Fast forward (300ms)"
          >
            <FastForward className="w-3.5 h-3.5" />
            <span>Fast (0.3s)</span>
          </button>
        )}
      </div>
    </div>
  );
};
